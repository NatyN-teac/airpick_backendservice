package com.airpick.airpick_service.controllers.admin;

import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.OfferResponseDto;
import com.airpick.airpick_service.models.Offer;
import com.airpick.airpick_service.models.OfferStatus;
import com.airpick.airpick_service.repositories.OfferRepository;
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
@RequestMapping("/api/v1/admin/offers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYS_ROLE')")
public class AdminOffersController {

    private final OfferRepository offerRepository;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<OfferResponseDto>>> listOffers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        Pageable p = PageRequest.of(page, size);
        var offersPage = (status == null || status.isBlank()) ?
                offerRepository.findAll(p) : offerRepository.findByStatus(OfferStatus.valueOf(status), p);

        List<OfferResponseDto> items = offersPage.stream().map(OfferResponseDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponseDto.ok(items));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<OfferResponseDto>> getOffer(@PathVariable UUID id) {
        Offer o = offerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        return ResponseEntity.ok(ApiResponseDto.ok(OfferResponseDto.from(o)));
    }

    @PatchMapping("/{id}/force-close")
    public ResponseEntity<ApiResponseDto<OfferResponseDto>> forceClose(@PathVariable UUID id) {
        Offer o = offerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        o.setStatus(OfferStatus.CLOSED);
        offerRepository.save(o);
        return ResponseEntity.ok(ApiResponseDto.ok(OfferResponseDto.from(o)));
    }

    @PatchMapping("/{id}/update-status")
    public ResponseEntity<ApiResponseDto<OfferResponseDto>> updateStatus(@PathVariable UUID id, @RequestBody java.util.Map<String, String> body) {
        String target = body.get("targetStatus");
        if (target == null) throw new IllegalArgumentException("targetStatus required");
        Offer o = offerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        o.setStatus(OfferStatus.valueOf(target));
        offerRepository.save(o);
        return ResponseEntity.ok(ApiResponseDto.ok(OfferResponseDto.from(o)));
    }
}
