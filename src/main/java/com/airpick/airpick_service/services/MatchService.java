package com.airpick.airpick_service.services;

import com.airpick.airpick_service.dtos.input.CancelMatchRequestDto;
import com.airpick.airpick_service.dtos.input.CreateMatchRequestDto;
import com.airpick.airpick_service.dtos.input.RejectMatchRequestDto;
import com.airpick.airpick_service.dtos.input.UpdateMatchedItemStatusRequestDto;
import com.airpick.airpick_service.dtos.output.MatchResponseDto;
import com.airpick.airpick_service.dtos.output.MatchTrackResponseDto;
import com.airpick.airpick_service.dtos.output.PickupPhotoUrlResponseDto;
import com.airpick.airpick_service.models.*;
import com.airpick.airpick_service.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for the full match lifecycle.
 * <p>
 * A shipper creates a match against an open offer. The carrier then accepts or rejects it.
 * Slot availability is enforced via {@link OfferItem#getRemainingQuantity()} — quantity
 * is decremented on match creation and restored on rejection or cancellation.
 * When all offer item slots are filled the parent offer transitions to FULLY_MATCHED;
 * it reverts to OPEN when quantities are restored.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchedItemRepository matchedItemRepository;
    private final MatchStatusHistoryRepository matchStatusHistoryRepository;
    private final MatchedItemStatusHistoryRepository matchedItemStatusHistoryRepository;
    private final MatchReceiverRepository matchReceiverRepository;
    private final ChatRepository chatRepository;
    private final OfferRepository offerRepository;
    private final OfferItemRepository offerItemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final GcsStorageService gcsStorageService;

    private static final long MAX_PICKUP_PHOTO_BYTES = 10 * 1024 * 1024;

    private static final List<MatchStatus> TRACK_STATUSES = List.of(
            MatchStatus.ACCEPTED,
            MatchStatus.IN_PROGRESS,
            MatchStatus.COMPLETED
    );

    // -------------------------------------------------------------------------
    // Shipper — create match
    // -------------------------------------------------------------------------

    /**
     * Creates a match request for the authenticated shipper against an open offer.
     * <p>
     * Validates per item that the requested quantity does not exceed the offer item's
     * remaining slots. Decrements remaining quantities atomically. If all slots are
     * filled the offer transitions to FULLY_MATCHED. A chat thread is created alongside
     * the match so carrier and shipper can communicate immediately.
     *
     * @param shipperEmail the authenticated shipper's email
     * @param request      the offer and item quantities being requested
     * @return the created match as a response DTO
     */
    @Transactional
    public MatchResponseDto createMatch(String shipperEmail, CreateMatchRequestDto request) {
        log.info("Shipper {} requesting match on offer {}", shipperEmail, request.offerId());

        User shipper = resolveUser(shipperEmail);

        Offer offer = offerRepository.findById(request.offerId())
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + request.offerId()));

        if (offer.getStatus() != OfferStatus.OPEN) {
            throw new IllegalArgumentException(
                    "Offer is not available for matching. Current status: " + offer.getStatus());
        }

        if (offer.getCarrier().getId().equals(shipper.getId())) {
            throw new IllegalArgumentException("Cannot match your own offer");
        }

        User carrier = offer.getCarrier();

        // Load all offer items into a map for O(1) lookup and end-of-method exhaustion check
        Map<UUID, OfferItem> offerItemMap = offerItemRepository.findAllByOfferId(offer.getId())
                .stream()
                .collect(Collectors.toMap(OfferItem::getId, Function.identity()));

        // Validate quantities and collect contexts
        List<MatchItemContext> contexts = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CreateMatchRequestDto.MatchItemDto dto : request.items()) {
            OfferItem offerItem = offerItemMap.get(dto.offerItemId());
            if (offerItem == null) {
                throw new IllegalArgumentException(
                        "Offer item not found on this offer: " + dto.offerItemId());
            }

            if (dto.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than 0 for item: " + offerItem.getItem().getName());
            }

            if (dto.quantity().compareTo(offerItem.getRemainingQuantity()) > 0) {
                throw new IllegalArgumentException(
                        "Requested quantity " + dto.quantity() +
                        " exceeds available slots " + offerItem.getRemainingQuantity() +
                        " for item: " + offerItem.getItem().getName());
            }

            offerItem.setRemainingQuantity(offerItem.getRemainingQuantity().subtract(dto.quantity()));
            totalPrice = totalPrice.add(dto.quantity().multiply(offerItem.getPricePerItem()));
            contexts.add(new MatchItemContext(offerItem, dto.quantity()));
        }

        offerItemRepository.saveAll(contexts.stream().map(MatchItemContext::offerItem).toList());
        log.info("Decremented quantities for {} offer items on offer {}", contexts.size(), offer.getId());

        // Check if all slots are now exhausted
        boolean allExhausted = offerItemMap.values().stream()
                .allMatch(oi -> oi.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0);
        if (allExhausted) {
            offer.setStatus(OfferStatus.FULLY_MATCHED);
            offerRepository.save(offer);
            log.info("Offer {} transitioned to FULLY_MATCHED", offer.getId());
        }

        // Validate receiver info when receiverNeeded = true
        if (request.receiverNeeded()) {
            CreateMatchRequestDto.ReceiverDto rd = request.receiver();
            if (rd == null || rd.firstName() == null || rd.lastName() == null
                    || rd.phone() == null || rd.photoIdUrl() == null) {
                throw new IllegalArgumentException(
                        "Receiver information (firstName, lastName, phone, photoIdUrl) is required when receiverNeeded is true");
            }
        }

        // Persist match
        Match match = Match.builder()
                .carrier(carrier)
                .shipper(shipper)
                .offer(offer)
                .status(MatchStatus.PENDING)
                .receiverNeeded(request.receiverNeeded())
                .totalPrice(totalPrice)
                .matchedItems(new ArrayList<>())
                .statusHistory(new ArrayList<>())
                .build();
        Match savedMatch = matchRepository.save(match);

        // Persist receiver if needed
        if (request.receiverNeeded()) {
            CreateMatchRequestDto.ReceiverDto rd = request.receiver();
            MatchReceiver receiver = MatchReceiver.builder()
                    .match(savedMatch)
                    .shipper(shipper)
                    .firstName(rd.firstName())
                    .lastName(rd.lastName())
                    .phone(rd.phone())
                    .photoIdUrl(rd.photoIdUrl())
                    .isActive(false)
                    .build();
            MatchReceiver savedReceiver = matchReceiverRepository.save(receiver);
            savedMatch.setReceiver(savedReceiver);
            log.info("Receiver created for match {} (pending activation)", savedMatch.getId());
        }

        // Persist matched items
        List<MatchedItem> matchedItems = new ArrayList<>();
        for (MatchItemContext ctx : contexts) {
            matchedItems.add(MatchedItem.builder()
                    .match(savedMatch)
                    .offerItem(ctx.offerItem())
                    .quantity(ctx.quantity())
                    .status(MatchedItemStatus.PENDING)
                    .statusHistory(new ArrayList<>())
                    .build());
        }
        List<MatchedItem> savedMatchedItems = matchedItemRepository.saveAll(matchedItems);
        savedMatch.getMatchedItems().addAll(savedMatchedItems);

        // Initial match status history
        recordMatchTransition(savedMatch, null, MatchStatus.PENDING, shipper, "Match request created");

        // Initial matched item status histories
        for (MatchedItem mi : savedMatchedItems) {
            recordMatchedItemTransition(mi, null, MatchedItemStatus.PENDING, shipper, "Matched item created");
        }

        // NOTE: Chat is created in acceptMatch, not here.
        log.info("Match {} created with totalPrice={} for offer {}", savedMatch.getId(), totalPrice, offer.getId());

        notificationService.notifyMatchCreated(savedMatch);
        if (allExhausted) {
            notificationService.notifyOfferFullyMatched(carrier, offer);
        }

        return MatchResponseDto.from(savedMatch);
    }

    // -------------------------------------------------------------------------
    // Carrier — lifecycle transitions
    // -------------------------------------------------------------------------

    /**
     * Carrier accepts a pending match request.
     * Transitions: PENDING → ACCEPTED.
     *
     * @param matchId      the ID of the match to accept
     * @param carrierEmail the authenticated carrier's email
     * @return the updated match
     */
    @Transactional
    public MatchResponseDto acceptMatch(UUID matchId, String carrierEmail) {
        log.info("Carrier {} accepting match {}", carrierEmail, matchId);

        User carrier = resolveUser(carrierEmail);
        Match match = resolveCarrierMatch(matchId, carrier);

        if (match.getStatus() != MatchStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING matches can be accepted. Current status: " + match.getStatus());
        }

        match.setStatus(MatchStatus.ACCEPTED);
        matchRepository.save(match);
        recordMatchTransition(match, MatchStatus.PENDING, MatchStatus.ACCEPTED, carrier, null);

        // Activate receiver if one was registered
        if (match.isReceiverNeeded() && match.getReceiver() != null) {
            match.getReceiver().setActive(true);
            matchReceiverRepository.save(match.getReceiver());
            log.info("Receiver activated for match {}", matchId);
        }

        // Create the chat room now that both parties have agreed
        Chat chat = Chat.builder()
                .match(match)
                .messages(new ArrayList<>())
                .build();
        chatRepository.save(chat);
        match.setChat(chat);

        // Push notification to shipper
        notificationService.notifyMatchAccepted(match);

        log.info("Match {} accepted by carrier {}", matchId, carrierEmail);
        return MatchResponseDto.from(match);
    }

    /**
     * Carrier rejects a pending match request with a mandatory rejection reason.
     * Restores the offer item quantities that were reserved for this match.
     * If the offer was FULLY_MATCHED it reverts to OPEN.
     * Transitions: PENDING → REJECTED. All matched items are set to CANCELLED.
     *
     * @param matchId      the ID of the match to reject
     * @param carrierEmail the authenticated carrier's email
     * @param request      must contain a non-blank rejection reason
     * @return the updated match
     */
    @Transactional
    public MatchResponseDto rejectMatch(UUID matchId, String carrierEmail, RejectMatchRequestDto request) {
        log.info("Carrier {} rejecting match {}", carrierEmail, matchId);

        if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }

        User carrier = resolveUser(carrierEmail);
        Match match = resolveCarrierMatch(matchId, carrier);

        if (match.getStatus() != MatchStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING matches can be rejected. Current status: " + match.getStatus());
        }

        restoreQuantities(match.getMatchedItems());
        checkAndRestoreOfferStatus(match.getOffer());

        match.setStatus(MatchStatus.REJECTED);
        matchRepository.save(match);
        recordMatchTransition(match, MatchStatus.PENDING, MatchStatus.REJECTED, carrier, request.rejectionReason());

        cancelMatchedItems(match.getMatchedItems(), carrier, "Rejected by carrier: " + request.rejectionReason());
        notificationService.notifyMatchRejected(match, request.rejectionReason());

        log.info("Match {} rejected by carrier {}. Reason: {}", matchId, carrierEmail, request.rejectionReason());
        return MatchResponseDto.from(match);
    }

    /**
     * Carrier marks the match as in-progress, indicating items have been collected.
     * Transitions: ACCEPTED → IN_PROGRESS. All matched items move to COLLECTED.
     *
     * @param matchId      the ID of the match to start
     * @param carrierEmail the authenticated carrier's email
     * @return the updated match
     */
    @Transactional
    public MatchResponseDto startMatch(UUID matchId, String carrierEmail) {
        log.info("Carrier {} starting match {}", carrierEmail, matchId);

        User carrier = resolveUser(carrierEmail);
        Match match = resolveCarrierMatch(matchId, carrier);

        if (match.getStatus() != MatchStatus.ACCEPTED) {
            throw new IllegalArgumentException(
                    "Only ACCEPTED matches can be started. Current status: " + match.getStatus());
        }

        if (match.getPickupPhotoObjectPath() == null) {
            throw new IllegalArgumentException(
                    "Pickup photo is required before starting the match. Upload via POST /matches/{matchId}/pickup-photo");
        }

        boolean hasPendingItems = match.getMatchedItems().stream()
                .anyMatch(mi -> mi.getStatus() == MatchedItemStatus.PENDING);
        if (hasPendingItems) {
            throw new IllegalArgumentException(
                    "All matched items must be collected before starting the match");
        }

        match.setStatus(MatchStatus.IN_PROGRESS);
        matchRepository.save(match);
        recordMatchTransition(match, MatchStatus.ACCEPTED, MatchStatus.IN_PROGRESS, carrier, "Items collected by carrier");

        notificationService.notifyMatchInProgress(match);

        log.info("Match {} started (IN_PROGRESS) by carrier {}", matchId, carrierEmail);
        return MatchResponseDto.from(match);
    }

    /**
     * Carrier marks the match as completed, indicating all items have been delivered.
     * Transitions: IN_PROGRESS → COMPLETED.
     * Any matched items not in a terminal state are automatically set to DELIVERED.
     *
     * @param matchId      the ID of the match to complete
     * @param carrierEmail the authenticated carrier's email
     * @return the updated match
     */
    @Transactional
    public MatchResponseDto completeMatch(UUID matchId, String carrierEmail) {
        log.info("Carrier {} completing match {}", carrierEmail, matchId);

        User carrier = resolveUser(carrierEmail);
        Match match = resolveCarrierMatch(matchId, carrier);

        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Only IN_PROGRESS matches can be completed. Current status: " + match.getStatus());
        }

        match.setStatus(MatchStatus.COMPLETED);
        matchRepository.save(match);
        recordMatchTransition(match, MatchStatus.IN_PROGRESS, MatchStatus.COMPLETED, carrier, "Delivery completed");

        for (MatchedItem mi : match.getMatchedItems()) {
            if (!isTerminalItemStatus(mi.getStatus())) {
                MatchedItemStatus prev = mi.getStatus();
                mi.setStatus(MatchedItemStatus.DELIVERED);
                matchedItemRepository.save(mi);
                recordMatchedItemTransition(mi, prev, MatchedItemStatus.DELIVERED, carrier, "Marked delivered on match completion");
            }
        }

        notificationService.notifyMatchDelivered(match);

        log.info("Match {} completed by carrier {}", matchId, carrierEmail);
        return MatchResponseDto.from(match);
    }

    // -------------------------------------------------------------------------
    // Either party — cancel
    // -------------------------------------------------------------------------

    /**
     * Cancels a match. Caller can be the shipper or the carrier.
     * <ul>
     *   <li>Shipper may cancel only when status is PENDING.</li>
     *   <li>Carrier may cancel when status is PENDING or ACCEPTED.</li>
     *   <li>If the carrier cancels after ACCEPTED, {@code carrier.cancellationCount} is incremented.</li>
     * </ul>
     * Restores offer item quantities. If the offer was FULLY_MATCHED it reverts to OPEN.
     * All matched items are set to CANCELLED.
     *
     * @param matchId   the ID of the match to cancel
     * @param userEmail the authenticated user's email (carrier or shipper)
     * @param request   optional note explaining the cancellation
     * @return the updated match
     */
    @Transactional
    public MatchResponseDto cancelMatch(UUID matchId, String userEmail, CancelMatchRequestDto request) {
        log.info("User {} cancelling match {}", userEmail, matchId);

        User user = resolveUser(userEmail);
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));

        boolean isCarrier = match.getCarrier().getId().equals(user.getId());
        boolean isShipper = match.getShipper().getId().equals(user.getId());

        if (!isCarrier && !isShipper) {
            throw new IllegalArgumentException("Match not found: " + matchId);
        }

        MatchStatus current = match.getStatus();

        if (isShipper && current != MatchStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Shippers can only cancel PENDING matches. Current status: " + current);
        }

        if (isCarrier && current != MatchStatus.PENDING && current != MatchStatus.ACCEPTED) {
            throw new IllegalArgumentException(
                    "Carriers can only cancel PENDING or ACCEPTED matches. Current status: " + current);
        }

        // Increment cancellation count if carrier cancels an already-accepted match
        if (isCarrier && current == MatchStatus.ACCEPTED) {
            User carrier = match.getCarrier();
            carrier.setCancellationCount(carrier.getCancellationCount() + 1);
            userRepository.save(carrier);
            log.info("Carrier {} cancellationCount incremented to {}", carrier.getId(), carrier.getCancellationCount());
        }

        restoreQuantities(match.getMatchedItems());
        checkAndRestoreOfferStatus(match.getOffer());

        String note = request != null && request.note() != null ? request.note() : "Cancelled by " + (isCarrier ? "carrier" : "shipper");
        match.setStatus(MatchStatus.CANCELLED);
        matchRepository.save(match);
        recordMatchTransition(match, current, MatchStatus.CANCELLED, user, note);

        cancelMatchedItems(match.getMatchedItems(), user, note);
        notificationService.notifyMatchCancelled(match, userEmail);

        log.info("Match {} cancelled by {} ({})", matchId, userEmail, isCarrier ? "carrier" : "shipper");
        return MatchResponseDto.from(match);
    }

    // -------------------------------------------------------------------------
    // Either party — update individual matched item status
    // -------------------------------------------------------------------------

    /**
     * Updates the delivery status of a single matched item within a match.
     * Both carrier and shipper of the match can call this endpoint.
     * Transitions from terminal statuses (DELIVERED, FAILED, RETURNED, CANCELLED) are not allowed.
     * Transitioning to CANCELLED is not allowed here — use cancel/reject instead.
     *
     * @param matchId        the ID of the parent match
     * @param matchedItemId  the ID of the matched item to update
     * @param userEmail      the authenticated user's email
     * @param request        the new status and optional note
     * @return the updated match
     */
    @Transactional
    public MatchResponseDto updateMatchedItemStatus(UUID matchId, UUID matchedItemId,
                                                    String userEmail, UpdateMatchedItemStatusRequestDto request) {
        log.info("User {} updating matched item {} status to {}", userEmail, matchedItemId, request.status());

        User user = resolveUser(userEmail);
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));

        boolean isParticipant = match.getCarrier().getId().equals(user.getId())
                || match.getShipper().getId().equals(user.getId());
        if (!isParticipant) {
            throw new IllegalArgumentException("Match not found: " + matchId);
        }

        MatchedItem matchedItem = matchedItemRepository.findById(matchedItemId)
                .orElseThrow(() -> new IllegalArgumentException("Matched item not found: " + matchedItemId));

        if (!matchedItem.getMatch().getId().equals(matchId)) {
            throw new IllegalArgumentException("Matched item does not belong to match: " + matchId);
        }

        if (isTerminalItemStatus(matchedItem.getStatus())) {
            throw new IllegalArgumentException(
                    "Cannot update a matched item in terminal status: " + matchedItem.getStatus());
        }

        if (request.status() == MatchedItemStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Use the cancel match endpoint to cancel matched items");
        }

        MatchedItemStatus previous = matchedItem.getStatus();
        matchedItem.setStatus(request.status());
        matchedItemRepository.save(matchedItem);
        recordMatchedItemTransition(matchedItem, previous, request.status(), user, request.note());

        log.info("Matched item {} updated from {} to {}", matchedItemId, previous, request.status());
        return MatchResponseDto.from(match);
    }

    // -------------------------------------------------------------------------
    // Carrier — pickup photo (private GCS)
    // -------------------------------------------------------------------------

    /**
     * Carrier uploads a pickup proof photo for the match. The file is streamed to a
     * private GCS bucket at {@code matches/{matchId}/pickup-photo.{ext}}.
     * All PENDING matched items are marked COLLECTED atomically.
     * Only allowed while the match is ACCEPTED (before IN_PROGRESS).
     */
    @Transactional
    public MatchResponseDto uploadPickupPhoto(UUID matchId, String carrierEmail, MultipartFile file) {
        log.info("Carrier {} uploading pickup photo for match {}", carrierEmail, matchId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photo file is required");
        }

        String contentType = file.getContentType();
        if (!GcsStorageService.isAllowedImageType(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported image type. Allowed: JPEG, PNG, WebP");
        }

        if (file.getSize() > MAX_PICKUP_PHOTO_BYTES) {
            throw new IllegalArgumentException("Photo file exceeds maximum size of 10 MB");
        }

        User carrier = resolveUser(carrierEmail);
        Match match = resolveCarrierMatch(matchId, carrier);

        if (match.getStatus() != MatchStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "Pickup photo can only be uploaded for ACCEPTED matches. Current status: " + match.getStatus());
        }

        if (match.getPickupPhotoObjectPath() != null) {
            gcsStorageService.deleteObject(match.getPickupPhotoObjectPath());
        }

        String objectPath;
        try (var inputStream = file.getInputStream()) {
            objectPath = gcsStorageService.uploadMatchPickupPhoto(
                    matchId, inputStream, contentType, file.getSize());
        } catch (IOException e) {
            log.error("Failed to stream pickup photo for match {}", matchId, e);
            throw new IllegalStateException("Failed to upload pickup photo");
        }

        match.setPickupPhotoObjectPath(objectPath);
        match.setPickupPhotoContentType(contentType);
        match.setPickupPhotoUploadedAt(java.time.LocalDateTime.now());
        matchRepository.save(match);

        for (MatchedItem mi : match.getMatchedItems()) {
            if (mi.getStatus() == MatchedItemStatus.PENDING) {
                mi.setStatus(MatchedItemStatus.COLLECTED);
                matchedItemRepository.save(mi);
                recordMatchedItemTransition(mi, MatchedItemStatus.PENDING, MatchedItemStatus.COLLECTED,
                        carrier, "Collected — pickup photo uploaded");
            }
        }

        log.info("Pickup photo saved for match {} at {}", matchId, objectPath);
        return MatchResponseDto.from(match);
    }

    /**
     * Returns a short-lived signed URL so carrier or shipper can view the private pickup photo.
     */
    @Transactional(readOnly = true)
    public PickupPhotoUrlResponseDto getPickupPhotoSignedUrl(UUID matchId, String userEmail) {
        User user = resolveUser(userEmail);
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));

        validateParticipant(match, user);

        if (match.getPickupPhotoObjectPath() == null) {
            throw new IllegalArgumentException("No pickup photo uploaded for this match");
        }

        GcsStorageService.SignedUrlResult signed = gcsStorageService.generateSignedReadUrl(
                match.getPickupPhotoObjectPath());

        return new PickupPhotoUrlResponseDto(
                match.getId(),
                signed.signedUrl(),
                signed.expiresAt(),
                match.getPickupPhotoContentType(),
                match.getPickupPhotoUploadedAt());
    }

    // -------------------------------------------------------------------------
    // Read methods
    // -------------------------------------------------------------------------

    /**
     * Returns a single match by ID.
     *
     * @param matchId the ID of the match
     * @return the match as a response DTO
     */
    @Transactional(readOnly = true)
    public MatchResponseDto getMatch(UUID matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));
        return MatchResponseDto.from(match);
    }

    /**
     * Returns all matches where the authenticated user is the shipper.
     *
     * @param email the authenticated shipper's email
     * @return list of matches ordered by creation date descending
     */
    @Transactional(readOnly = true)
    public List<MatchResponseDto> getMyMatchesAsShipper(String email) {
        User shipper = resolveUser(email);
        log.info("Fetching shipper matches for user {}", shipper.getId());
        return matchRepository.findAllByShipperId(shipper.getId())
                .stream()
                .map(MatchResponseDto::from)
                .toList();
    }

    /**
     * Returns all matches where the authenticated user is the carrier.
     *
     * @param email the authenticated carrier's email
     * @return list of matches ordered by creation date descending
     */
    @Transactional(readOnly = true)
    public List<MatchResponseDto> getMyMatchesAsCarrier(String email) {
        User carrier = resolveUser(email);
        log.info("Fetching carrier matches for user {}", carrier.getId());
        return matchRepository.findAllByCarrierId(carrier.getId())
                .stream()
                .map(MatchResponseDto::from)
                .toList();
    }

    /**
     * Returns delivery tracking for matches where the user is the shipper,
     * grouped into collected (ACCEPTED), inProgress (IN_PROGRESS), and completed (COMPLETED).
     */
    @Transactional(readOnly = true)
    public MatchTrackResponseDto getTrackAsShipper(String email) {
        User shipper = resolveUser(email);
        log.info("Fetching shipper track for user {}", shipper.getId());
        return buildTrack(matchRepository.findAllByShipperIdAndStatusInOrderByUpdatedAtDesc(
                shipper.getId(), TRACK_STATUSES));
    }

    /**
     * Returns delivery tracking for matches where the user is the carrier,
     * grouped into collected (ACCEPTED), inProgress (IN_PROGRESS), and completed (COMPLETED).
     */
    @Transactional(readOnly = true)
    public MatchTrackResponseDto getTrackAsCarrier(String email) {
        User carrier = resolveUser(email);
        log.info("Fetching carrier track for user {}", carrier.getId());
        return buildTrack(matchRepository.findAllByCarrierIdAndStatusInOrderByUpdatedAtDesc(
                carrier.getId(), TRACK_STATUSES));
    }

    /**
     * Returns all matches against a specific offer. Caller must be the carrier who owns the offer.
     *
     * @param offerId      the ID of the offer
     * @param carrierEmail the authenticated carrier's email
     * @return list of matches on this offer ordered by creation date descending
     */
    @Transactional(readOnly = true)
    public List<MatchResponseDto> getMatchesByOffer(UUID offerId, String carrierEmail) {
        User carrier = resolveUser(carrierEmail);

        boolean ownsOffer = offerRepository.findById(offerId)
                .map(o -> o.getCarrier().getId().equals(carrier.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + offerId));

        if (!ownsOffer) {
            throw new IllegalArgumentException("Offer not found: " + offerId);
        }

        log.info("Fetching matches for offer {} by carrier {}", offerId, carrier.getId());
        return matchRepository.findAllByOfferId(offerId)
                .stream()
                .map(MatchResponseDto::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", email);
                    return new IllegalArgumentException("User not found: " + email);
                });
    }

    private MatchTrackResponseDto buildTrack(List<Match> matches) {
        List<MatchResponseDto> collected = new ArrayList<>();
        List<MatchResponseDto> inProgress = new ArrayList<>();
        List<MatchResponseDto> completed = new ArrayList<>();

        for (Match match : matches) {
            MatchResponseDto dto = MatchResponseDto.from(match);
            switch (match.getStatus()) {
                case ACCEPTED -> collected.add(dto);
                case IN_PROGRESS -> inProgress.add(dto);
                case COMPLETED -> completed.add(dto);
                default -> { /* excluded by query */ }
            }
        }

        return new MatchTrackResponseDto(collected, inProgress, completed);
    }

    /**
     * Loads the match and validates the caller is its carrier.
     * Throws 404-style error on ownership mismatch to avoid leaking existence.
     */
    private Match resolveCarrierMatch(UUID matchId, User carrier) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));

        if (!match.getCarrier().getId().equals(carrier.getId())) {
            log.warn("Ownership mismatch — match {} does not belong to carrier {}", matchId, carrier.getId());
            throw new IllegalArgumentException("Match not found: " + matchId);
        }

        return match;
    }

    private void validateParticipant(Match match, User user) {
        boolean isParticipant = match.getCarrier().getId().equals(user.getId())
                || match.getShipper().getId().equals(user.getId());
        if (!isParticipant) {
            log.warn("User {} attempted to access match {} without being a participant",
                    user.getId(), match.getId());
            throw new IllegalArgumentException("Match not found: " + match.getId());
        }
    }

    /**
     * Restores {@code remainingQuantity} on each offer item for the quantities consumed by these matched items.
     */
    private void restoreQuantities(List<MatchedItem> matchedItems) {
        for (MatchedItem mi : matchedItems) {
            OfferItem offerItem = mi.getOfferItem();
            offerItem.setRemainingQuantity(offerItem.getRemainingQuantity().add(mi.getQuantity()));
            offerItemRepository.save(offerItem);
            log.debug("Restored {} units to offer item {}", mi.getQuantity(), offerItem.getId());
        }
    }

    /**
     * If the offer is FULLY_MATCHED but now has available slots (after a restore), reverts it to OPEN.
     */
    private void checkAndRestoreOfferStatus(Offer offer) {
        if (offer.getStatus() == OfferStatus.FULLY_MATCHED) {
            List<OfferItem> items = offerItemRepository.findAllByOfferId(offer.getId());
            boolean hasSlots = items.stream()
                    .anyMatch(oi -> oi.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0);
            if (hasSlots) {
                offer.setStatus(OfferStatus.OPEN);
                offerRepository.save(offer);
                log.info("Offer {} reverted to OPEN after quantity restore", offer.getId());
            }
        }
    }

    /**
     * Appends a {@link MatchStatusHistory} entry for a match status transition.
     */
    private void recordMatchTransition(Match match, MatchStatus from, MatchStatus to, User changedBy, String note) {
        matchStatusHistoryRepository.save(MatchStatusHistory.builder()
                .match(match)
                .previousStatus(from)
                .newStatus(to)
                .changedBy(changedBy)
                .note(note)
                .build());
    }

    /**
     * Appends a {@link MatchedItemStatusHistory} entry for a matched item status transition.
     */
    private void recordMatchedItemTransition(MatchedItem item, MatchedItemStatus from,
                                             MatchedItemStatus to, User changedBy, String note) {
        matchedItemStatusHistoryRepository.save(MatchedItemStatusHistory.builder()
                .matchedItem(item)
                .previousStatus(from)
                .newStatus(to)
                .changedBy(changedBy)
                .note(note)
                .build());
    }

    /**
     * Sets all non-terminal matched items to CANCELLED and records their history.
     */
    private void cancelMatchedItems(List<MatchedItem> matchedItems, User changedBy, String note) {
        for (MatchedItem mi : matchedItems) {
            if (!isTerminalItemStatus(mi.getStatus())) {
                MatchedItemStatus prev = mi.getStatus();
                mi.setStatus(MatchedItemStatus.CANCELLED);
                matchedItemRepository.save(mi);
                recordMatchedItemTransition(mi, prev, MatchedItemStatus.CANCELLED, changedBy, note);
            }
        }
    }

    /**
     * Terminal item statuses — no further transitions are allowed from these.
     */
    private boolean isTerminalItemStatus(MatchedItemStatus status) {
        return status == MatchedItemStatus.DELIVERED
                || status == MatchedItemStatus.FAILED
                || status == MatchedItemStatus.RETURNED
                || status == MatchedItemStatus.CANCELLED;
    }

    /** Internal context for match creation — pairs an offer item with its requested quantity. */
    private record MatchItemContext(OfferItem offerItem, BigDecimal quantity) {}
}
