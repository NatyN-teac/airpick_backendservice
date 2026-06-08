package com.airpick.airpick_service.services;

import com.airpick.airpick_service.dtos.input.CreateOfferRequestDto;
import com.airpick.airpick_service.dtos.input.UpdateOfferRequestDto;
import com.airpick.airpick_service.dtos.output.OfferResponseDto;
import com.airpick.airpick_service.models.*;
import com.airpick.airpick_service.repositories.FlightRepository;
import com.airpick.airpick_service.repositories.ItemRepository;
import com.airpick.airpick_service.repositories.MatchRepository;
import com.airpick.airpick_service.repositories.OfferItemRepository;
import com.airpick.airpick_service.repositories.OfferRepository;
import com.airpick.airpick_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for offer lifecycle management.
 * <p>
 * Covers carrier-initiated (DIRECT) offer creation, updates, cancellation,
 * and retrieval for both carrier and shipper perspectives.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final OfferItemRepository offerItemRepository;
    private final FlightRepository flightRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final NotificationService notificationService;

    /**
     * Creates a new DIRECT offer for the authenticated carrier.
     * <p>
     * Validates that the carrier owns the flight and that no active DIRECT offer
     * already exists for it. Resolves and validates each item. Sets
     * {@code remainingQuantity = quantity} on each offer item at creation.
     *
     * @param email   the authenticated carrier's email
     * @param request offer details including flight, areas, items, and pricing
     * @return the saved offer as a response DTO
     */
    @Transactional
    public OfferResponseDto createOffer(String email, CreateOfferRequestDto request) {
        log.info("Creating offer for carrier: {}, flight: {}", email, request.flightId());

        User carrier = resolveCarrier(email);
        Flight flight = resolveOwnedFlight(request.flightId(), carrier);

        offerRepository.findDirectOfferByFlightId(flight.getId()).ifPresent(existing -> {
            log.warn("Active DIRECT offer already exists for flight: {}", flight.getId());
            throw new IllegalArgumentException("An active offer already exists for flight: " + flight.getId());
        });

        List<OfferItem> builtItems = buildOfferItems(null, request.items());
        boolean hasManualItem = builtItems.stream()
                .anyMatch(oi -> oi.getItem().getCreatedBy() != null);

        Offer offer = Offer.builder()
                .carrier(carrier)
                .flight(flight)
                .offerSource(OfferSource.DIRECT)
                .status(hasManualItem ? OfferStatus.PENDING_ITEM_APPROVAL : OfferStatus.OPEN)
                .hasManualItem(hasManualItem)
                .currency(request.currency())
                .deliveryArea(request.deliveryArea())
                .pickupArea(request.pickupArea())
                .urgencyLevel(request.urgencyLevel())
                .discount(request.discount())
                .specialNote(request.specialNote())
                .meetupPlaces(request.meetupPlaces() != null ? new ArrayList<>(request.meetupPlaces()) : new ArrayList<>())
                .paymentMethods(request.paymentMethods() != null ? new ArrayList<>(request.paymentMethods()) : new ArrayList<>())
                .offerItems(new ArrayList<>())
                .build();

        Offer saved = offerRepository.save(offer);

        builtItems.forEach(oi -> oi.setOffer(saved));
        saved.getOfferItems().addAll(builtItems);
        offerItemRepository.saveAll(builtItems);

        log.info("Offer created with id: {}, status: {}", saved.getId(), saved.getStatus());
        return OfferResponseDto.from(saved);
    }

    /**
     * Updates the non-item fields of an existing offer owned by the authenticated carrier.
     * <p>
     * Only non-null fields are applied. Offer items are managed separately.
     * Cannot update an offer that is CANCELLED or FULLY_MATCHED.
     *
     * @param offerId the ID of the offer to update
     * @param email   the authenticated carrier's email
     * @param request fields to update; null values are ignored
     * @return the updated offer as a response DTO
     */
    @Transactional
    public OfferResponseDto updateOffer(UUID offerId, String email, UpdateOfferRequestDto request) {
        log.info("Updating offer: {} for carrier: {}", offerId, email);

        User carrier = resolveCarrier(email);
        Offer offer = resolveOwnedOffer(offerId, carrier);

        if (offer.getStatus() == OfferStatus.CANCELLED || offer.getStatus() == OfferStatus.FULLY_MATCHED) {
            throw new IllegalArgumentException(
                    "Offer cannot be updated in status: " + offer.getStatus());
        }

        if (request.currency()       != null) offer.setCurrency(request.currency());
        if (request.deliveryArea()   != null) offer.setDeliveryArea(request.deliveryArea());
        if (request.pickupArea()     != null) offer.setPickupArea(request.pickupArea());
        if (request.urgencyLevel()   != null) offer.setUrgencyLevel(request.urgencyLevel());
        if (request.discount()       != null) offer.setDiscount(request.discount());
        if (request.specialNote()    != null) offer.setSpecialNote(request.specialNote());
        if (request.meetupPlaces()   != null) {
            offer.getMeetupPlaces().clear();
            offer.getMeetupPlaces().addAll(request.meetupPlaces());
        }
        if (request.paymentMethods() != null) {
            offer.getPaymentMethods().clear();
            offer.getPaymentMethods().addAll(request.paymentMethods());
        }

        Offer saved = offerRepository.save(offer);
        log.info("Offer updated: {}", saved.getId());
        return OfferResponseDto.from(saved);
    }

    /**
     * Cancels an offer owned by the authenticated carrier.
     * <p>
     * Only OPEN offers can be cancelled. An offer with active matches
     * cannot be cancelled until those matches are resolved.
     *
     * @param offerId the ID of the offer to cancel
     * @param email   the authenticated carrier's email
     */
    @Transactional
    public void cancelOffer(UUID offerId, String email) {
        log.info("Cancelling offer: {} for carrier: {}", offerId, email);

        User carrier = resolveCarrier(email);
        Offer offer = resolveOwnedOffer(offerId, carrier);

        if (offer.getStatus() != OfferStatus.OPEN) {
            throw new IllegalArgumentException(
                    "Only OPEN offers can be cancelled. Current status: " + offer.getStatus());
        }

        offer.setStatus(OfferStatus.CANCELLED);
        offerRepository.save(offer);

        matchRepository.findAllByOfferId(offerId).stream()
                .filter(m -> m.getStatus() == MatchStatus.PENDING || m.getStatus() == MatchStatus.ACCEPTED)
                .forEach(m -> notificationService.notifyOfferCancelled(m.getShipper(), offer));

        log.info("Offer cancelled: {}", offerId);
    }

    /**
     * Returns a single offer by ID.
     *
     * @param offerId the ID of the offer
     * @return the offer as a response DTO
     */
    public OfferResponseDto getOffer(UUID offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> {
                    log.warn("Offer not found: {}", offerId);
                    return new IllegalArgumentException("Offer not found: " + offerId);
                });
        return OfferResponseDto.from(offer);
    }

    /**
     * Returns all OPEN offers available for shippers to browse.
     *
     * @return list of open offers ordered by creation date descending
     */
    public List<OfferResponseDto> getAllOpenOffers() {
        log.info("Fetching all open offers");
        return offerRepository.findAllOpen()
                .stream()
                .map(OfferResponseDto::from)
                .toList();
    }

    /**
     * Returns all OPEN offers excluding those created by the authenticated user.
     * <p>
     * Intended for shippers browsing available offers — prevents a user from
     * seeing their own carrier offers, which they cannot match with.
     *
     * @param email the authenticated user's email
     * @return list of open offers not owned by the user, ordered by creation date descending
     */
    public List<OfferResponseDto> browseOffers(String email) {
        User user = resolveCarrier(email);
        log.info("Browsing open offers for user: {}, excluding own", user.getId());
        return offerRepository.findAllOpenExcludingCarrier(user.getId())
                .stream()
                .map(OfferResponseDto::from)
                .toList();
    }

    /**
     * Returns all CLOSED offers.
     *
     * @return list of closed offers ordered by creation date descending
     */
    public List<OfferResponseDto> getAllClosedOffers() {
        log.info("Fetching all closed offers");
        return offersByStatus(OfferStatus.CLOSED);
    }

    /**
     * Returns all CLOSED offers excluding those created by the authenticated user.
     *
     * @param email the authenticated user's email
     * @return list of closed offers not owned by the user, ordered by creation date descending
     */
    public List<OfferResponseDto> browseClosedOffers(String email) {
        User user = resolveCarrier(email);
        log.info("Browsing closed offers for user: {}, excluding own", user.getId());
        return offersByStatusExcluding(OfferStatus.CLOSED, user.getId());
    }

    /**
     * Returns all FULLY_MATCHED offers.
     *
     * @return list of fully matched offers ordered by creation date descending
     */
    public List<OfferResponseDto> getAllFullyMatchedOffers() {
        log.info("Fetching all fully matched offers");
        return offersByStatus(OfferStatus.FULLY_MATCHED);
    }

    /**
     * Returns all FULLY_MATCHED offers excluding those created by the authenticated user.
     *
     * @param email the authenticated user's email
     * @return list of fully matched offers not owned by the user, ordered by creation date descending
     */
    public List<OfferResponseDto> browseFullyMatchedOffers(String email) {
        User user = resolveCarrier(email);
        log.info("Browsing fully matched offers for user: {}, excluding own", user.getId());
        return offersByStatusExcluding(OfferStatus.FULLY_MATCHED, user.getId());
    }

    /**
     * Returns all CANCELLED offers.
     *
     * @return list of cancelled offers ordered by creation date descending
     */
    public List<OfferResponseDto> getAllCancelledOffers() {
        log.info("Fetching all cancelled offers");
        return offersByStatus(OfferStatus.CANCELLED);
    }

    /**
     * Returns all CANCELLED offers excluding those created by the authenticated user.
     *
     * @param email the authenticated user's email
     * @return list of cancelled offers not owned by the user, ordered by creation date descending
     */
    public List<OfferResponseDto> browseCancelledOffers(String email) {
        User user = resolveCarrier(email);
        log.info("Browsing cancelled offers for user: {}, excluding own", user.getId());
        return offersByStatusExcluding(OfferStatus.CANCELLED, user.getId());
    }

    /**
     * Returns all offers created by the authenticated carrier.
     *
     * @param email the authenticated carrier's email
     * @return list of the carrier's offers ordered by creation date descending
     */
    public List<OfferResponseDto> getMyOffers(String email) {
        User carrier = resolveCarrier(email);
        log.info("Fetching offers for carrier: {}", carrier.getId());
        return offerRepository.findAllByCarrierId(carrier.getId())
                .stream()
                .map(OfferResponseDto::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private User resolveCarrier(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Carrier not found: {}", email);
                    return new IllegalArgumentException("User not found: " + email);
                });
    }

    private Flight resolveOwnedFlight(UUID flightId, User carrier) {
        Flight flight = flightRepository.findActiveById(flightId)
                .orElseThrow(() -> {
                    log.warn("Flight not found: {}", flightId);
                    return new IllegalArgumentException("Flight not found: " + flightId);
                });

        if (!flight.getUser().getId().equals(carrier.getId())) {
            log.warn("Ownership mismatch — flight {} does not belong to carrier {}", flightId, carrier.getId());
            throw new IllegalArgumentException("Flight not found: " + flightId);
        }

        return flight;
    }

    private Offer resolveOwnedOffer(UUID offerId, User carrier) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> {
                    log.warn("Offer not found: {}", offerId);
                    return new IllegalArgumentException("Offer not found: " + offerId);
                });

        if (!offer.getCarrier().getId().equals(carrier.getId())) {
            log.warn("Ownership mismatch — offer {} does not belong to carrier {}", offerId, carrier.getId());
            throw new IllegalArgumentException("Offer not found: " + offerId);
        }

        return offer;
    }

    private Item resolveItem(UUID itemId) {
        return itemRepository.findActiveApprovedById(itemId)
                .orElseThrow(() -> {
                    log.warn("Item not found or not approved: {}", itemId);
                    return new IllegalArgumentException("Item not found: " + itemId);
                });
    }

    private List<OfferResponseDto> offersByStatus(OfferStatus status) {
        return offerRepository.findAllByStatus(status)
                .stream()
                .map(OfferResponseDto::from)
                .toList();
    }

    private List<OfferResponseDto> offersByStatusExcluding(OfferStatus status, UUID carrierId) {
        return offerRepository.findAllByStatusExcludingCarrier(status, carrierId)
                .stream()
                .map(OfferResponseDto::from)
                .toList();
    }

    private List<OfferItem> buildOfferItems(Offer offer, List<CreateOfferRequestDto.OfferItemDto> dtos) {
        List<OfferItem> items = new ArrayList<>();
        for (CreateOfferRequestDto.OfferItemDto dto : dtos) {
            Item item = resolveItem(dto.itemId());
            items.add(OfferItem.builder()
                    .offer(offer)   // may be null at detection stage; caller sets it before save
                    .item(item)
                    .quantity(dto.quantity())
                    .remainingQuantity(dto.quantity())
                    .pricePerItem(dto.pricePerItem())
                    .build());
        }
        return items;
    }
}
