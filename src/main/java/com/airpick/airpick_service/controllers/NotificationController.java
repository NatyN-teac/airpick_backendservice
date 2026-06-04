package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.commons.security.UserDetailsImpl;
import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.NotificationResponseDto;
import com.airpick.airpick_service.models.Notification;
import com.airpick.airpick_service.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for the notification inbox.
 * <p>
 * All notifications are persisted server-side. The mobile app fetches missed
 * notifications on launch, polls unread counts for badge display, and marks
 * notifications as read when the user views them.
 * <p>
 * Each notification includes {@code refType} + {@code refId} for deep-linking:
 * <pre>
 *   refType = "MATCH"         → navigate to MatchDetailScreen(refId)
 *   refType = "OFFER_REQUEST" → navigate to OfferRequestScreen(refId)
 * </pre>
 */
@Tag(
        name = "Notifications",
        description = "Notification inbox management. " +
                      "Notifications are persisted server-side and pushed via FCM. " +
                      "Use GET /notifications to load the inbox on app start, " +
                      "GET /notifications/unread-count for badge display, " +
                      "and PATCH endpoints to mark as read. " +
                      "Each notification carries refType + refId for deep-link navigation."
)
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Get notification inbox",
            description = "Returns a paginated list of all notifications for the authenticated user, " +
                          "newest first. Use page=0&size=20 for initial load. " +
                          "Each notification includes refType and refId for in-app navigation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications returned",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<NotificationResponseDto>>> getNotifications(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> result = notificationService.getNotificationsForUser(
                principal.getId(), pageable);

        List<NotificationResponseDto> dtos = result.getContent().stream()
                .map(NotificationResponseDto::from)
                .toList();

        return ResponseEntity.ok(ApiResponseDto.ok(dtos));
    }

    @Operation(
            summary = "Get unread notification count",
            description = "Returns the number of unread notifications. " +
                          "Use this for the notification badge in the app nav bar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Count returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponseDto<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal UserDetailsImpl principal) {

        long count = notificationService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(ApiResponseDto.ok(Map.of("count", count)));
    }

    @Operation(
            summary = "Mark a notification as read",
            description = "Marks a single notification as read. " +
                          "Call this when the user taps a notification or views its target screen."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Marked as read"),
            @ApiResponse(responseCode = "403", description = "Notification belongs to another user"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Parameter(description = "Notification ID") @PathVariable UUID id) {

        notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks every unread notification for the authenticated user as read. " +
                          "Call this when the user opens the notification inbox screen."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All notifications marked as read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponseDto<Map<String, Integer>>> markAllAsRead(
            @AuthenticationPrincipal UserDetailsImpl principal) {

        int updated = notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(ApiResponseDto.ok(Map.of("markedRead", updated)));
    }
}
