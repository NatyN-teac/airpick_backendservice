package com.airpick.airpick_service.controllers.admin;

import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYS_ROLE')")
public class AdminNotificationsController {

    private final NotificationService notificationService;

    @PostMapping
    public ApiResponseDto<Void> send(@RequestBody Map<String, Object> body) {
        // body: { target: { userIds: ["..."], roles: ["SYS_ROLE"], all: true }, title, body, data }
        Map<String, Object> target = (Map<String, Object>) body.get("target");
        String title = (String) body.get("title");
        String b = (String) body.get("body");

        Map<String, String> data = null;
        if (body.get("data") instanceof Map) {
            data = ((Map<?, ?>) body.get("data")).entrySet().stream()
                    .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));
        }

        if (target == null) throw new IllegalArgumentException("target required");

        if (Boolean.TRUE.equals(target.get("all"))) {
            notificationService.sendSystemNotificationToAll(title, b, data);
            return ApiResponseDto.ok();
        }

        if (target.get("roles") instanceof List) {
            List<String> roles = (List<String>) target.get("roles");
            for (String role : roles) notificationService.sendSystemNotificationToRole(role, title, b, data);
        }

        if (target.get("userIds") instanceof List) {
            List<String> ids = (List<String>) target.get("userIds");
            List<UUID> uuids = ids.stream().map(UUID::fromString).collect(Collectors.toList());
            notificationService.sendSystemNotificationToUsers(uuids, title, b, data);
        }

        return ApiResponseDto.ok();
    }
}
