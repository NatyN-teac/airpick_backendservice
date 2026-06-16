package com.airpick.airpick_service.controllers.admin;

import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.SenderOfferRequestResponseDto;
import com.airpick.airpick_service.models.OfferRequest;
import com.airpick.airpick_service.models.OfferRequestStatus;
import com.airpick.airpick_service.repositories.OfferRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/offer-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYS_ROLE')")
public class AdminOfferRequestsController {

    private final OfferRequestRepository offerRequestRepository;
    private final com.airpick.airpick_service.services.ItemService itemService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<SenderOfferRequestResponseDto>>> listRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        Pageable p = PageRequest.of(page, size);
        var pageRes = (status == null || status.isBlank()) ?
                offerRequestRepository.findAll(p) : offerRequestRepository.findByStatus(com.airpick.airpick_service.models.OfferRequestStatus.valueOf(status), p);

        List<SenderOfferRequestResponseDto> items = pageRes.stream().map(SenderOfferRequestResponseDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponseDto.ok(items));
    }

    @PatchMapping("/{id}/approve-items")
    public ResponseEntity<ApiResponseDto<SenderOfferRequestResponseDto>> approveManualItems(@PathVariable UUID id) {
        OfferRequest r = offerRequestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Offer request not found: " + id));
        r.getItems().stream()
                .map(it -> it.getItem())
                .filter(item -> item.getCreatedBy() != null && !item.isApproved())
                .forEach(item -> itemService.approveItem(item.getId()));

        boolean hasManual = r.getItems().stream()
                .anyMatch(it -> it.getItem().getCreatedBy() != null && !it.getItem().isApproved());
        r.setHasManualItem(hasManual);
        if (!hasManual) r.setStatus(OfferRequestStatus.OPEN);
        offerRequestRepository.save(r);
        return ResponseEntity.ok(ApiResponseDto.ok(SenderOfferRequestResponseDto.from(r)));
    }

    @PatchMapping("/{id}/reject-items")
    public ResponseEntity<ApiResponseDto<SenderOfferRequestResponseDto>> rejectManualItems(@PathVariable UUID id) {
        OfferRequest r = offerRequestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Offer request not found: " + id));
        r.getItems().stream()
                .map(it -> it.getItem())
                .filter(item -> item.getCreatedBy() != null && !item.isApproved())
                .forEach(item -> itemService.rejectItem(item.getId()));

        r.setHasManualItem(false);
        r.setStatus(OfferRequestStatus.CANCELLED);
        offerRequestRepository.save(r);
        return ResponseEntity.ok(ApiResponseDto.ok(SenderOfferRequestResponseDto.from(r)));
    }
}
