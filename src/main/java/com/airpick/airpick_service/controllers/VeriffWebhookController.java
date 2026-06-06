package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.services.IdentityVerificationService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public webhook endpoints called by Veriff. Secured via HMAC signature validation, not JWT.
 */
@Slf4j
@Hidden
@Tag(name = "Veriff Webhooks", description = "Internal endpoints for Veriff callbacks")
@RestController
@RequestMapping("/api/v1/webhooks/veriff")
@RequiredArgsConstructor
public class VeriffWebhookController {

    private final IdentityVerificationService identityVerificationService;

    @Operation(summary = "Veriff decision webhook")
    @PostMapping("/decisions")
    public ResponseEntity<Void> handleDecision(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-HMAC-SIGNATURE", required = false) String signature) {
        identityVerificationService.handleDecisionWebhook(rawBody, signature);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Veriff event webhook")
    @PostMapping("/events")
    public ResponseEntity<Void> handleEvent(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-HMAC-SIGNATURE", required = false) String signature) {
        identityVerificationService.handleEventWebhook(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
