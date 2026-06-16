package com.airpick.airpick_service.services;

import com.airpick.airpick_service.commons.security.VeriffHmacService;
import com.airpick.airpick_service.dtos.output.VerificationSessionResponseDto;
import com.airpick.airpick_service.dtos.output.VerificationStatusResponseDto;
import com.airpick.airpick_service.integrations.veriff.VeriffClient;
import com.airpick.airpick_service.integrations.veriff.VeriffSessionRequest;
import com.airpick.airpick_service.integrations.veriff.VeriffSessionResult;
import com.airpick.airpick_service.models.User;
import com.airpick.airpick_service.models.UserProfile;
import com.airpick.airpick_service.models.UserVerification;
import com.airpick.airpick_service.repositories.UserProfileRepository;
import com.airpick.airpick_service.repositories.UserRepository;
import com.airpick.airpick_service.repositories.UserVerificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityVerificationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String NOT_STARTED = "NOT_STARTED";
    private static final Set<String> FINAL_NEGATIVE = Set.of(
            "declined", "expired", "abandoned", "resubmission_requested");

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final VeriffClient veriffClient;
    private final VeriffHmacService veriffHmacService;

    @Transactional
    public VerificationSessionResponseDto createSession(String email) {
        User user = findUserByEmail(email);
        UserProfile profile = findProfile(user);

        log.info("Processing verification session for userId={}, email={}, isVerified={}",
                user.getId(), email, profile.isVerified());

        if (profile.isVerified()) {
            log.warn("Verification session rejected — user already verified: userId={}, email={}",
                    user.getId(), email);
            throw new IllegalStateException("User is already verified");
        }

        VeriffSessionResult result = veriffClient.createSession(new VeriffSessionRequest(
                user.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getDob()
        ));

        UserVerification verification = userVerificationRepository.findByUserProfileId(profile.getId())
                .orElseGet(() -> UserVerification.builder()
                        .userProfile(profile)
                        .build());

        verification.setVeriffSessionId(result.sessionId());
        verification.setStatus(result.status() != null ? result.status() : "created");
        verification.setVerificationRequestedAt(LocalDateTime.now());
        verification.setDenied(false);
        verification.setVerificationMessage(null);
        verification.setVerifiedAt(null);
        userVerificationRepository.save(verification);

        log.info("Verification session saved for userId={}, veriffSessionId={}, status={}",
                user.getId(), result.sessionId(), verification.getStatus());

        return new VerificationSessionResponseDto(
                UUID.fromString(result.sessionId()),
                result.sessionUrl(),
                result.sessionToken(),
                verification.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public VerificationStatusResponseDto getStatus(String email) {
        User user = findUserByEmail(email);
        UserProfile profile = findProfile(user);

        return userVerificationRepository.findByUserProfileId(profile.getId())
                .map(v -> new VerificationStatusResponseDto(
                        v.getStatus() != null ? v.getStatus() : NOT_STARTED,
                        profile.isVerified(),
                        v.isDenied(),
                        v.getVerificationMessage(),
                        v.getVeriffSessionId(),
                        v.getVerificationRequestedAt(),
                        v.getVerifiedAt()))
                .orElseGet(() -> new VerificationStatusResponseDto(
                        NOT_STARTED,
                        profile.isVerified(),
                        false,
                        null,
                        null,
                        null,
                        null));
    }

    @Transactional
    public void handleDecisionWebhook(String rawBody, String signature) {
        validateWebhookSignature(rawBody, signature);

        JsonNode root = parseJson(rawBody);
        JsonNode verification = root.get("verification");
        if (verification == null || verification.isNull()) {
            log.warn("Veriff decision webhook missing verification object");
            return;
        }

        String sessionId = textOrNull(verification, "id");
        String endUserId = textOrNull(verification, "endUserId");
        String status = textOrNull(verification, "status");
        String reason = textOrNull(verification, "reason");

        log.info("Veriff decision webhook: sessionId={}, endUserId={}, status={}", sessionId, endUserId, status);

        UserVerification userVerification = resolveVerification(sessionId, endUserId);
        if (userVerification == null) {
            log.warn("No local verification record for Veriff session {}", sessionId);
            return;
        }

        UserProfile profile = userVerification.getUserProfile();
        userVerification.setStatus(status);
        userVerification.setVerificationMessage(reason);

        switch (status != null ? status : "") {
            case "approved" -> {
                profile.setVerified(true);
                userVerification.setDenied(false);
                userVerification.setVerifiedAt(LocalDateTime.now());
            }
            case "declined" -> {
                profile.setVerified(false);
                userVerification.setDenied(true);
                userVerification.setVerifiedAt(null);
            }
            case "resubmission_requested" -> {
                profile.setVerified(false);
                userVerification.setDenied(false);
                userVerification.setVerifiedAt(null);
            }
            default -> {
                if (FINAL_NEGATIVE.contains(status)) {
                    profile.setVerified(false);
                    userVerification.setVerifiedAt(null);
                }
            }
        }

        userProfileRepository.save(profile);
        userVerificationRepository.save(userVerification);
    }

    @Transactional
    public void handleEventWebhook(String rawBody, String signature) {
        validateWebhookSignature(rawBody, signature);

        JsonNode root = parseJson(rawBody);
        String action = textOrNull(root, "action");
        String sessionId = textOrNull(root, "id");
        if (sessionId == null) {
            sessionId = textOrNull(root.path("verification"), "id");
        }

        log.info("Veriff event webhook: sessionId={}, action={}", sessionId, action);
        if (sessionId == null || action == null) {
            return;
        }

        userVerificationRepository.findByVeriffSessionIdWithProfile(sessionId).ifPresent(verification -> {
            verification.setStatus(action);
            userVerificationRepository.save(verification);
        });
    }

    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> listVerifications(int page, int size, String status) {
        org.springframework.data.domain.Pageable p = org.springframework.data.domain.PageRequest.of(page, size);
        var pageRes = (status == null || status.isBlank()) ? userVerificationRepository.findAll(p) : userVerificationRepository.findByStatus(status, p);
        return pageRes.stream().map(uv -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("sessionId", uv.getVeriffSessionId());
            m.put("userId", uv.getUserProfile().getUser().getId());
            m.put("status", uv.getStatus());
            m.put("createdAt", uv.getVerificationRequestedAt());
            m.put("verifiedAt", uv.getVerifiedAt());
            return m;
        }).toList();
    }

    @Transactional
    public void overrideVerification(String sessionId, String overrideStatus) {
        UserVerification uv = userVerificationRepository.findByVeriffSessionIdWithProfile(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found: " + sessionId));

        UserProfile profile = uv.getUserProfile();
        uv.setStatus(overrideStatus);
        uv.setVerificationMessage("Overridden by admin");

        if ("verified".equalsIgnoreCase(overrideStatus) || "approved".equalsIgnoreCase(overrideStatus)) {
            profile.setVerified(true);
            uv.setDenied(false);
            uv.setVerifiedAt(java.time.LocalDateTime.now());
        } else {
            profile.setVerified(false);
            uv.setDenied(true);
            uv.setVerifiedAt(null);
        }

        userProfileRepository.save(profile);
        userVerificationRepository.save(uv);
    }

    private UserVerification resolveVerification(String sessionId, String endUserId) {
        if (sessionId != null) {
            var bySession = userVerificationRepository.findByVeriffSessionIdWithProfile(sessionId);
            if (bySession.isPresent()) {
                return bySession.get();
            }
        }

        if (endUserId == null) {
            return null;
        }

        try {
            UUID userId = UUID.fromString(endUserId);
            return userProfileRepository.findByUserId(userId)
                    .flatMap(profile -> userVerificationRepository.findByUserProfileId(profile.getId()))
                    .orElse(null);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid endUserId in Veriff webhook: {}", endUserId);
            return null;
        }
    }

    private void validateWebhookSignature(String rawBody, String signature) {
        if (!veriffHmacService.isValidWebhookSignature(rawBody, signature)) {
            throw new IllegalArgumentException("Invalid Veriff webhook signature");
        }
    }

    private JsonNode parseJson(String rawBody) {
        try {
            return OBJECT_MAPPER.readTree(rawBody);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Veriff webhook payload", e);
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    private UserProfile findProfile(User user) {
        return userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + user.getEmail()));
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
