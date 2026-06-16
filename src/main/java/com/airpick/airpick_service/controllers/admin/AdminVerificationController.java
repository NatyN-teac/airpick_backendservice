package com.airpick.airpick_service.controllers.admin;

import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.VerificationSessionResponseDto;
import com.airpick.airpick_service.models.UserVerification;
import com.airpick.airpick_service.repositories.UserVerificationRepository;
import com.airpick.airpick_service.services.IdentityVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/verifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYS_ROLE')")
public class AdminVerificationController {

    private final UserVerificationRepository userVerificationRepository;
    private final IdentityVerificationService identityVerificationService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<Map<String, Object>>>> list(@RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "20") int size,
                                                                            @RequestParam(required = false) String status) {
        Pageable p = PageRequest.of(page, size);
        var pageRes = (status == null || status.isBlank()) ? userVerificationRepository.findAll(p) : userVerificationRepository.findByStatus(status, p);

        List<Map<String, Object>> items = pageRes.stream().map(uv -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("sessionId", uv.getVeriffSessionId());
            m.put("userId", uv.getUserProfile().getUser().getId());
            m.put("status", uv.getStatus());
            m.put("createdAt", uv.getVerificationRequestedAt());
            m.put("verifiedAt", uv.getVerifiedAt());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponseDto.ok(items));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> get(@PathVariable String sessionId) {
        UserVerification uv = userVerificationRepository.findByVeriffSessionIdWithProfile(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found: " + sessionId));
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("sessionId", uv.getVeriffSessionId());
        m.put("userId", uv.getUserProfile().getUser().getId());
        m.put("status", uv.getStatus());
        m.put("message", uv.getVerificationMessage());
        m.put("createdAt", uv.getVerificationRequestedAt());
        m.put("verifiedAt", uv.getVerifiedAt());
        return ResponseEntity.ok(ApiResponseDto.ok(m));
    }

    @PatchMapping("/{sessionId}")
    public ResponseEntity<ApiResponseDto<Void>> override(@PathVariable String sessionId, @RequestBody Map<String, String> body) {
        String overrideStatus = body.get("overrideStatus");
        if (overrideStatus == null) throw new IllegalArgumentException("overrideStatus required");

        // Admin override: update verification record and user's profile
        identityVerificationService.overrideVerification(sessionId, overrideStatus);
        return ResponseEntity.ok(ApiResponseDto.ok());
    }
}
