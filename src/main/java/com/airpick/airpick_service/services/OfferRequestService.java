package com.airpick.airpick_service.services;

import com.airpick.airpick_service.dtos.input.CreateSenderOfferRequestDto;
import com.airpick.airpick_service.dtos.input.UpdateSenderOfferRequestDto;
import com.airpick.airpick_service.dtos.output.SenderOfferRequestResponseDto;
import com.airpick.airpick_service.models.*;
import com.airpick.airpick_service.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service managing the sender's offer request lifecycle.
 * <p>
 * A sender creates an offer request to broadcast their shipping need to carriers.
 * Carriers browse open requests and submit {@link OfferProposal}s with their price.
 * When the sender accepts a proposal, this request transitions to {@link OfferRequestStatus#ACCEPTED}
 * and an Offer + Match is created atomically (see {@link OfferProposalService#acceptProposal}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferRequestService {

    private final OfferRequestRepository offerRequestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    // -------------------------------------------------------------------------
    // Sender — create request
    // -------------------------------------------------------------------------

    @Transactional
    public SenderOfferRequestResponseDto createRequest(String email, CreateSenderOfferRequestDto dto) {
        log.info("Creating offer request for shipper: {}", email);

        User shipper = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        if (dto.items() == null || dto.items().isEmpty()) {
            throw new IllegalArgumentException("Offer request must have at least one item");
        }
        if (dto.sourceCity() == null || dto.sourceCity().isBlank()) {
            throw new IllegalArgumentException("Source city is required");
        }

        OfferRequest request = OfferRequest.builder()
                .shipper(shipper)
                .sourceCountry(dto.sourceCountry())
                .sourceCity(dto.sourceCity())
                .destinationCountry(dto.destinationCountry())
                .preferredDate(dto.preferredDate())
                .urgencyLevel(dto.urgencyLevel())
                .specialNote(dto.specialNote())
                .partialProposalAccepted(dto.partialProposalAccepted())
                .status(OfferRequestStatus.OPEN)
                .items(new ArrayList<>())
                .proposals(new ArrayList<>())
                .build();

        List<OfferRequestItem> items = dto.items().stream()
                .map(itemDto -> buildRequestItem(request, itemDto))
                .toList();

        boolean hasManualItem = items.stream()
                .anyMatch(i -> i.getItem().getCreatedBy() != null);

        request.setHasManualItem(hasManualItem);
        request.setStatus(hasManualItem ? OfferRequestStatus.PENDING_ITEM_APPROVAL : OfferRequestStatus.OPEN);
        request.getItems().addAll(items);

        OfferRequest saved = offerRequestRepository.save(request);
        log.info("Offer request created: {}", saved.getId());
        return SenderOfferRequestResponseDto.from(saved);
    }

    // -------------------------------------------------------------------------
    // Sender — update request
    // -------------------------------------------------------------------------

    @Transactional
    public SenderOfferRequestResponseDto updateRequest(String email, UUID id, UpdateSenderOfferRequestDto dto) {
        log.info("Updating offer request {} by: {}", id, email);

        OfferRequest request = findByIdAndShipper(id, email);

        if (request.getStatus() == OfferRequestStatus.CANCELLED
                || request.getStatus() == OfferRequestStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot update an offer request with status: " + request.getStatus());
        }

        if (dto.sourceCity() != null) request.setSourceCity(dto.sourceCity());
        if (dto.preferredDate() != null) request.setPreferredDate(dto.preferredDate());
        if (dto.urgencyLevel() != null) request.setUrgencyLevel(dto.urgencyLevel());
        if (dto.specialNote() != null) request.setSpecialNote(dto.specialNote());
        if (dto.partialProposalAccepted() != null) request.setPartialProposalAccepted(dto.partialProposalAccepted());

        OfferRequest saved = offerRequestRepository.save(request);
        log.info("Offer request updated: {}", saved.getId());
        return SenderOfferRequestResponseDto.from(saved);
    }

    // -------------------------------------------------------------------------
    // Sender — cancel request
    // -------------------------------------------------------------------------

    @Transactional
    public void cancelRequest(String email, UUID id) {
        log.info("Cancelling offer request {} by: {}", id, email);

        OfferRequest request = findByIdAndShipper(id, email);

        if (request.getStatus() == OfferRequestStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot cancel an already accepted offer request");
        }
        if (request.getStatus() == OfferRequestStatus.CANCELLED) {
            throw new IllegalStateException("Offer request is already cancelled");
        }

        request.setStatus(OfferRequestStatus.CANCELLED);
        offerRequestRepository.save(request);
        log.info("Offer request cancelled: {}", id);
    }

    // -------------------------------------------------------------------------
    // Read — single
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SenderOfferRequestResponseDto getRequest(UUID id) {
        OfferRequest request = offerRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer request not found: " + id));
        return SenderOfferRequestResponseDto.from(request);
    }

    // -------------------------------------------------------------------------
    // Read — lists
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SenderOfferRequestResponseDto> getAllOpenRequests() {
        return offerRequestRepository.findAllOpen().stream()
                .map(SenderOfferRequestResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SenderOfferRequestResponseDto> browseRequests(String email) {
        User shipper = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        return offerRequestRepository.findAllOpenExcludingShipper(shipper.getId()).stream()
                .map(SenderOfferRequestResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SenderOfferRequestResponseDto> getMyRequests(String email) {
        User shipper = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        return offerRequestRepository.findAllByShipperId(shipper.getId()).stream()
                .map(SenderOfferRequestResponseDto::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private OfferRequest findByIdAndShipper(UUID id, String email) {
        OfferRequest request = offerRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer request not found: " + id));
        if (!request.getShipper().getEmail().equals(email)) {
            throw new IllegalArgumentException("Offer request does not belong to this user");
        }
        return request;
    }

    private OfferRequestItem buildRequestItem(OfferRequest request, CreateSenderOfferRequestDto.RequestItemDto dto) {
        Item item = itemRepository.findById(dto.itemId())
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + dto.itemId()));
        if (!item.isActive() || !item.isApproved()) {
            throw new IllegalArgumentException("Item is not available: " + item.getName());
        }
        return OfferRequestItem.builder()
                .offerRequest(request)
                .item(item)
                .quantity(dto.quantity())
                .build();
    }
}
