package com.airpick.airpick_service.services;

import com.airpick.airpick_service.dtos.input.AcceptProposalRequestDto;
import com.airpick.airpick_service.dtos.input.SubmitProposalRequestDto;
import com.airpick.airpick_service.dtos.output.MatchResponseDto;
import com.airpick.airpick_service.dtos.output.OfferProposalResponseDto;
import com.airpick.airpick_service.models.*;
import com.airpick.airpick_service.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service managing the full proposal lifecycle.
 * <p>
 * A carrier submits a {@link OfferProposal} in response to a sender's {@link OfferRequest}.
 * The proposal lists per-item prices for the items in the request.
 * <p>
 * If {@code partialProposalAccepted} is false on the request, the carrier must price every item.
 * If true, a subset is allowed.
 * <p>
 * When the sender accepts a proposal, this service atomically:
 * <ol>
 *   <li>Accepts the proposal and rejects all other pending proposals on the same request.</li>
 *   <li>Marks the offer request as ACCEPTED.</li>
 *   <li>Creates an {@link Offer} with {@link OfferSource#FROM_PROPOSAL} and status FULLY_MATCHED.</li>
 *   <li>Creates an {@link OfferItem} per proposal item (pricePerItem from proposal, quantity from request item).</li>
 *   <li>Creates a {@link Match} with status ACCEPTED immediately.</li>
 *   <li>Creates {@link MatchedItem}s for each offer item.</li>
 *   <li>Creates a {@link Chat} for the match.</li>
 *   <li>Creates a {@link MatchReceiver} if receiverNeeded.</li>
 *   <li>Notifies the carrier their proposal was accepted.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferProposalService {

    private final OfferProposalRepository offerProposalRepository;
    private final OfferProposalItemRepository offerProposalItemRepository;
    private final OfferRequestRepository offerRequestRepository;
    private final OfferRequestItemRepository offerRequestItemRepository;
    private final OfferRepository offerRepository;
    private final OfferItemRepository offerItemRepository;
    private final MatchRepository matchRepository;
    private final MatchedItemRepository matchedItemRepository;
    private final MatchStatusHistoryRepository matchStatusHistoryRepository;
    private final MatchedItemStatusHistoryRepository matchedItemStatusHistoryRepository;
    private final MatchReceiverRepository matchReceiverRepository;
    private final ChatRepository chatRepository;
    private final FlightRepository flightRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // -------------------------------------------------------------------------
    // Carrier — submit proposal
    // -------------------------------------------------------------------------

    @Transactional
    public OfferProposalResponseDto submitProposal(String email, UUID requestId, SubmitProposalRequestDto dto) {
        log.info("Carrier {} submitting proposal for request: {}", email, requestId);

        User carrier = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        OfferRequest offerRequest = offerRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Offer request not found: " + requestId));

        if (offerRequest.getStatus() != OfferRequestStatus.OPEN
                && offerRequest.getStatus() != OfferRequestStatus.PROPOSAL_RECEIVED) {
            throw new IllegalStateException("Cannot submit proposal for request with status: " + offerRequest.getStatus());
        }

        if (offerRequest.getShipper().getId().equals(carrier.getId())) {
            throw new IllegalArgumentException("Cannot submit a proposal on your own offer request");
        }

        offerProposalRepository.findActiveByOfferRequestIdAndCarrierId(requestId, carrier.getId())
                .ifPresent(existing -> {
                    throw new IllegalStateException("You already have an active proposal for this request");
                });

        if (dto.meetupPlaces() == null || dto.meetupPlaces().isEmpty()) {
            throw new IllegalArgumentException("At least one meetup place is required");
        }
        if (dto.paymentMethods() == null || dto.paymentMethods().isEmpty()) {
            throw new IllegalArgumentException("At least one payment method is required");
        }
        if (dto.items() == null || dto.items().isEmpty()) {
            throw new IllegalArgumentException("Proposal must include at least one item");
        }

        Flight flight = flightRepository.findById(dto.flightId())
                .orElseThrow(() -> new IllegalArgumentException("Flight not found: " + dto.flightId()));

        if (!flight.getUser().getId().equals(carrier.getId())) {
            throw new IllegalArgumentException("Flight does not belong to this carrier");
        }

        validateItemCoverage(offerRequest, dto);

        OfferProposal proposal = OfferProposal.builder()
                .offerRequest(offerRequest)
                .carrier(carrier)
                .flight(flight)
                .status(OfferProposalStatus.PENDING)
                .deliveryArea(dto.deliveryArea())
                .pickupArea(dto.pickupArea())
                .discount(dto.discount())
                .meetupPlaces(new ArrayList<>(dto.meetupPlaces()))
                .paymentMethods(new ArrayList<>(dto.paymentMethods()))
                .note(dto.note())
                .items(new ArrayList<>())
                .build();

        Map<UUID, OfferRequestItem> requestItemMap = offerRequest.getItems().stream()
                .collect(Collectors.toMap(OfferRequestItem::getId, Function.identity()));

        List<OfferProposalItem> proposalItems = dto.items().stream()
                .map(itemDto -> {
                    OfferRequestItem requestItem = requestItemMap.get(itemDto.offerRequestItemId());
                    if (requestItem == null) {
                        throw new IllegalArgumentException(
                                "OfferRequestItem not found in this request: " + itemDto.offerRequestItemId());
                    }
                    return OfferProposalItem.builder()
                            .proposal(proposal)
                            .offerRequestItem(requestItem)
                            .pricePerItem(itemDto.pricePerItem())
                            .build();
                })
                .toList();

        proposal.getItems().addAll(proposalItems);

        // Update request status to PROPOSAL_RECEIVED if still OPEN
        if (offerRequest.getStatus() == OfferRequestStatus.OPEN) {
            offerRequest.setStatus(OfferRequestStatus.PROPOSAL_RECEIVED);
            offerRequestRepository.save(offerRequest);
        }

        OfferProposal saved = offerProposalRepository.save(proposal);
        log.info("Proposal {} created for request {}", saved.getId(), requestId);

        notificationService.notifyNewProposal(saved);

        return OfferProposalResponseDto.from(saved);
    }

    // -------------------------------------------------------------------------
    // Carrier — withdraw proposal
    // -------------------------------------------------------------------------

    @Transactional
    public void withdrawProposal(String email, UUID proposalId) {
        log.info("Carrier {} withdrawing proposal: {}", email, proposalId);

        OfferProposal proposal = findByIdAndCarrier(proposalId, email);

        if (proposal.getStatus() != OfferProposalStatus.PENDING) {
            throw new IllegalStateException("Can only withdraw a PENDING proposal, current status: " + proposal.getStatus());
        }

        proposal.setStatus(OfferProposalStatus.WITHDRAWN);
        offerProposalRepository.save(proposal);
        log.info("Proposal {} withdrawn by carrier: {}", proposalId, email);
    }

    // -------------------------------------------------------------------------
    // Sender — reject proposal
    // -------------------------------------------------------------------------

    @Transactional
    public void rejectProposal(String email, UUID proposalId) {
        log.info("Sender {} rejecting proposal: {}", email, proposalId);

        OfferProposal proposal = findByIdAndShipper(proposalId, email);

        if (proposal.getStatus() != OfferProposalStatus.PENDING) {
            throw new IllegalStateException("Can only reject a PENDING proposal, current status: " + proposal.getStatus());
        }

        proposal.setStatus(OfferProposalStatus.REJECTED);
        offerProposalRepository.save(proposal);
        log.info("Proposal {} rejected by shipper: {}", proposalId, email);
    }

    // -------------------------------------------------------------------------
    // Sender — accept proposal (atomic: Offer + Match + Chat + MatchReceiver)
    // -------------------------------------------------------------------------

    @Transactional
    public MatchResponseDto acceptProposal(String email, UUID proposalId, AcceptProposalRequestDto dto) {
        log.info("Sender {} accepting proposal: {}", email, proposalId);

        OfferProposal proposal = findByIdAndShipper(proposalId, email);

        if (proposal.getStatus() != OfferProposalStatus.PENDING) {
            throw new IllegalStateException("Can only accept a PENDING proposal, current status: " + proposal.getStatus());
        }

        if (dto.receiverNeeded() && dto.receiver() == null) {
            throw new IllegalArgumentException("Receiver details are required when receiverNeeded is true");
        }

        OfferRequest offerRequest = proposal.getOfferRequest();
        User shipper = offerRequest.getShipper();
        User carrier = proposal.getCarrier();

        // 1. Accept this proposal, reject all others
        proposal.setStatus(OfferProposalStatus.ACCEPTED);
        offerProposalRepository.save(proposal);

        List<OfferProposal> otherPending = offerProposalRepository.findPendingByOfferRequestId(offerRequest.getId())
                .stream()
                .filter(p -> !p.getId().equals(proposalId))
                .toList();
        otherPending.forEach(p -> p.setStatus(OfferProposalStatus.REJECTED));
        offerProposalRepository.saveAll(otherPending);
        log.info("Rejected {} other proposals on request {}", otherPending.size(), offerRequest.getId());

        // 2. Mark offer request as ACCEPTED
        offerRequest.setStatus(OfferRequestStatus.ACCEPTED);
        offerRequestRepository.save(offerRequest);

        // 3. Build Offer (FROM_PROPOSAL, FULLY_MATCHED)
        String deliveryArea = proposal.getDeliveryArea() != null
                ? proposal.getDeliveryArea()
                : offerRequest.getDestinationCountry();
        String pickupArea = proposal.getPickupArea() != null
                ? proposal.getPickupArea()
                : offerRequest.getSourceCountry();

        Offer offer = Offer.builder()
                .carrier(carrier)
                .flight(proposal.getFlight())
                .status(OfferStatus.FULLY_MATCHED)
                .offerSource(OfferSource.FROM_PROPOSAL)
                .offerRequest(offerRequest)
                .deliveryArea(deliveryArea)
                .pickupArea(pickupArea)
                .urgencyLevel(offerRequest.getUrgencyLevel())
                .discount(proposal.getDiscount())
                .specialNote(proposal.getNote())
                .meetupPlaces(new ArrayList<>(proposal.getMeetupPlaces()))
                .paymentMethods(new ArrayList<>(proposal.getPaymentMethods()))
                .offerItems(new ArrayList<>())
                .build();

        Offer savedOffer = offerRepository.save(offer);

        // 4. Build OfferItems from proposal items
        List<OfferItem> offerItems = proposal.getItems().stream()
                .map(pi -> OfferItem.builder()
                        .offer(savedOffer)
                        .item(pi.getOfferRequestItem().getItem())
                        .quantity(pi.getOfferRequestItem().getQuantity())
                        .remainingQuantity(BigDecimal.ZERO)  // fully matched
                        .pricePerItem(pi.getPricePerItem())
                        .build())
                .toList();

        offerItemRepository.saveAll(offerItems);
        savedOffer.getOfferItems().addAll(offerItems);

        // 5. Calculate total price
        BigDecimal totalPrice = offerItems.stream()
                .map(oi -> oi.getQuantity().multiply(oi.getPricePerItem()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6. Build Match (ACCEPTED immediately — proposal flow skips PENDING)
        Match match = Match.builder()
                .carrier(carrier)
                .shipper(shipper)
                .offer(savedOffer)
                .status(MatchStatus.ACCEPTED)
                .receiverNeeded(dto.receiverNeeded())
                .totalPrice(totalPrice)
                .matchedItems(new ArrayList<>())
                .statusHistory(new ArrayList<>())
                .build();

        Match savedMatch = matchRepository.save(match);

        // 7. Build MatchedItems
        List<MatchedItem> matchedItems = offerItems.stream()
                .map(oi -> MatchedItem.builder()
                        .match(savedMatch)
                        .offerItem(oi)
                        .quantity(oi.getQuantity())
                        .status(MatchedItemStatus.PENDING)
                        .statusHistory(new ArrayList<>())
                        .build())
                .toList();

        matchedItemRepository.saveAll(matchedItems);
        savedMatch.getMatchedItems().addAll(matchedItems);

        // Record matched item status history
        matchedItems.forEach(mi -> {
            MatchedItemStatusHistory h = MatchedItemStatusHistory.builder()
                    .matchedItem(mi)
                    .previousStatus(null)
                    .newStatus(MatchedItemStatus.PENDING)
                    .changedBy(shipper)
                    .build();
            matchedItemStatusHistoryRepository.save(h);
        });

        // 8. Record match status history (no PENDING → ACCEPTED transition; accepted from birth)
        MatchStatusHistory matchHistory = MatchStatusHistory.builder()
                .match(savedMatch)
                .previousStatus(null)
                .newStatus(MatchStatus.ACCEPTED)
                .changedBy(shipper)
                .build();
        matchStatusHistoryRepository.save(matchHistory);
        savedMatch.getStatusHistory().add(matchHistory);

        // 9. Create MatchReceiver if needed (isActive = true since match is already accepted)
        if (dto.receiverNeeded()) {
            AcceptProposalRequestDto.ReceiverDto rd = dto.receiver();
            MatchReceiver receiver = MatchReceiver.builder()
                    .match(savedMatch)
                    .shipper(shipper)
                    .firstName(rd.firstName())
                    .lastName(rd.lastName())
                    .phone(rd.phone())
                    .photoIdUrl(rd.photoIdUrl())
                    .isActive(true)
                    .build();
            matchReceiverRepository.save(receiver);
            savedMatch.setReceiver(receiver);
            log.info("MatchReceiver created (active) for match: {}", savedMatch.getId());
        }

        // 10. Create Chat
        Chat chat = Chat.builder()
                .match(savedMatch)
                .messages(new ArrayList<>())
                .build();
        chatRepository.save(chat);
        savedMatch.setChat(chat);

        log.info("Proposal {} accepted. Offer {}, Match {}, Chat {} created atomically.",
                proposalId, savedOffer.getId(), savedMatch.getId(), chat.getId());

        // 11. Notify carrier
        notificationService.notifyProposalAccepted(proposal, savedMatch.getId());

        return MatchResponseDto.from(savedMatch);
    }

    // -------------------------------------------------------------------------
    // Read — proposals for a request
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<OfferProposalResponseDto> getProposalsForRequest(String email, UUID requestId) {
        OfferRequest offerRequest = offerRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Offer request not found: " + requestId));

        if (!offerRequest.getShipper().getEmail().equals(email)) {
            throw new IllegalArgumentException("Offer request does not belong to this user");
        }

        return offerProposalRepository.findAllByOfferRequestId(requestId).stream()
                .map(OfferProposalResponseDto::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Read — carrier's own proposals
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<OfferProposalResponseDto> getMyProposals(String email) {
        User carrier = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        return offerProposalRepository.findAllByCarrierId(carrier.getId()).stream()
                .map(OfferProposalResponseDto::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private OfferProposal findByIdAndCarrier(UUID proposalId, String email) {
        OfferProposal proposal = offerProposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
        if (!proposal.getCarrier().getEmail().equals(email)) {
            throw new IllegalArgumentException("Proposal does not belong to this carrier");
        }
        return proposal;
    }

    private OfferProposal findByIdAndShipper(UUID proposalId, String email) {
        OfferProposal proposal = offerProposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
        if (!proposal.getOfferRequest().getShipper().getEmail().equals(email)) {
            throw new IllegalArgumentException("Proposal does not belong to this shipper");
        }
        return proposal;
    }

    /**
     * Validates that the proposal's items cover all (or an acceptable subset of) the request's items.
     * <p>
     * If {@code partialProposalAccepted} is false, every item in the offer request must be priced.
     * If true, at least one item must be priced and all priced items must belong to this request.
     */
    private void validateItemCoverage(OfferRequest offerRequest, SubmitProposalRequestDto dto) {
        Set<UUID> requestItemIds = offerRequest.getItems().stream()
                .map(OfferRequestItem::getId)
                .collect(Collectors.toSet());

        Set<UUID> proposedItemIds = dto.items().stream()
                .map(SubmitProposalRequestDto.ProposalItemDto::offerRequestItemId)
                .collect(Collectors.toSet());

        // All proposed items must belong to this request
        for (UUID proposedId : proposedItemIds) {
            if (!requestItemIds.contains(proposedId)) {
                throw new IllegalArgumentException(
                        "Proposal item " + proposedId + " does not belong to offer request " + offerRequest.getId());
            }
        }

        if (!offerRequest.isPartialProposalAccepted()) {
            // Full coverage required
            if (!proposedItemIds.containsAll(requestItemIds)) {
                throw new IllegalArgumentException(
                        "This offer request does not accept partial proposals — you must price all "
                                + requestItemIds.size() + " items");
            }
        }
        // Partial is fine — at least 1 item already guaranteed by earlier check
    }
}
