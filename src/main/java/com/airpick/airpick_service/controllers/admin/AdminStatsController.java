package com.airpick.airpick_service.controllers.admin;

import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYS_ROLE')")
public class AdminStatsController {

    private final com.airpick.airpick_service.repositories.UserRepository userRepository;
    private final com.airpick.airpick_service.repositories.OfferRepository offerRepository;
    private final com.airpick.airpick_service.repositories.ItemRepository itemRepository;
    private final com.airpick.airpick_service.repositories.FlightRepository flightRepository;
    private final com.airpick.airpick_service.repositories.UserVerificationRepository userVerificationRepository;

    @GetMapping("/overview")
    public ApiResponseDto<Map<String, Object>> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveUserTrue();
        long blockedUsers = userRepository.countByIsBlockedTrue();

        Map<String, Long> offersByStatus = Arrays.stream(com.airpick.airpick_service.models.OfferStatus.values())
                .collect(Collectors.toMap(Enum::name, s -> offerRepository.countByStatus(s)));

        long pendingItems = itemRepository.findAllPendingApproval().size();
        long flights = flightRepository.count();
        long verificationsPending = userVerificationRepository.countByStatus("pending");

        m.put("totalUsers", totalUsers);
        m.put("activeUsers", activeUsers);
        m.put("blockedUsers", blockedUsers);
        m.put("offersByStatus", offersByStatus);
        m.put("pendingItems", pendingItems);
        m.put("flights", flights);
        m.put("verificationsPending", verificationsPending);

        return ApiResponseDto.ok(m);
    }

    @GetMapping("/daily")
    public ApiResponseDto<List<Map<String, Object>>> daily(@RequestParam String metric,
                                                          @RequestParam String from,
                                                          @RequestParam String to) {
        LocalDate f = LocalDate.parse(from);
        LocalDate t = LocalDate.parse(to);
        List<Map<String, Object>> series = new ArrayList<>();
        List<LocalDate> days = f.datesUntil(t.plusDays(1)).toList();

        switch (metric) {
            case "users": {
                var all = userRepository.findAll();
                Map<LocalDate, Long> counts = all.stream().collect(Collectors.groupingBy(u -> u.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate(), Collectors.counting()));
                for (LocalDate d : days) series.add(Map.of("date", d.toString(), "count", counts.getOrDefault(d, 0L)));
                break;
            }
            case "offers": {
                var all = offerRepository.findAll();
                Map<LocalDate, Long> counts = all.stream().collect(Collectors.groupingBy(o -> o.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate(), Collectors.counting()));
                for (LocalDate d : days) series.add(Map.of("date", d.toString(), "count", counts.getOrDefault(d, 0L)));
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported metric: " + metric);
        }

        return ApiResponseDto.ok(series);
    }
}
