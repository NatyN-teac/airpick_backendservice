package com.airpick.airpick_service.controllers.admin;

import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.PaginationDto;
import com.airpick.airpick_service.dtos.output.UserResponseDto;
import com.airpick.airpick_service.models.User;
import com.airpick.airpick_service.repositories.UserRepository;
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
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYS_ROLE')")
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<ApiResponseDto<List<UserResponseDto>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        Pageable p = PageRequest.of(page, size);
        var usersPage = (search == null || search.isBlank()) ?
                userRepository.findAll(p) : userRepository.findByEmailContainingIgnoreCase(search, p);

        List<UserResponseDto> items = usersPage.stream().map(u -> UserResponseDto.from(u, null)).collect(Collectors.toList());
        PaginationDto pagination = new PaginationDto(page, size, usersPage.getTotalElements(), usersPage.getTotalPages());

        return ResponseEntity.ok(ApiResponseDto.paged(items, pagination));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> getUser(@PathVariable UUID id) {
        User u = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        return ResponseEntity.ok(ApiResponseDto.ok(UserResponseDto.from(u, null)));
    }

    @PatchMapping("/users/{id}/block")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> blockUser(@PathVariable UUID id, @RequestBody java.util.Map<String, Boolean> body) {
        boolean blocked = Boolean.TRUE.equals(body.get("blocked"));
        User u = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        u.setBlocked(blocked);
        userRepository.save(u);
        return ResponseEntity.ok(ApiResponseDto.ok(UserResponseDto.from(u, null)));
    }

    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> activateUser(@PathVariable UUID id, @RequestBody java.util.Map<String, Boolean> body) {
        boolean active = Boolean.TRUE.equals(body.get("active"));
        User u = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        u.setActiveUser(active);
        userRepository.save(u);
        return ResponseEntity.ok(ApiResponseDto.ok(UserResponseDto.from(u, null)));
    }

}
