package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.commons.security.UserDetailsImpl;
import com.airpick.airpick_service.dtos.input.CreateItemRequestDto;
import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.ItemResponseDto;
import com.airpick.airpick_service.services.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(
        name = "Items",
        description = "Item catalogue management. " +
                      "Admin-seeded items are always approved and appear in the catalogue. " +
                      "Carriers can submit custom items which start as pending and require admin approval " +
                      "before they appear in the catalogue and can be used in offers."
)
@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    // -------------------------------------------------------------------------
    // Public catalogue
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get all available items",
            description = "Returns all active, approved items ordered by category then name. " +
                          "Use the returned item IDs when creating an offer or offer request."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Items retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<ItemResponseDto>>> getAllItems() {
        return ResponseEntity.ok(ApiResponseDto.ok(itemService.getAllItems()));
    }

    // -------------------------------------------------------------------------
    // Carrier — submit custom item
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Submit a custom item for approval",
            description = "Carrier submits a custom item that is not in the existing catalogue. " +
                          "The item starts as pending (isApproved=false) and will not appear in the catalogue " +
                          "until an admin approves it. The carrier will be notified once a decision is made."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item submitted, pending admin approval",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<ItemResponseDto>> submitItem(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody CreateItemRequestDto request) {
        ItemResponseDto item = itemService.submitItem(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.ok(item));
    }

    // -------------------------------------------------------------------------
    // Admin — review pending items
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get all pending items (admin)",
            description = "Returns all items submitted by carriers that are awaiting admin approval. " +
                          "Admin use only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending items retrieved",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/pending")
    public ResponseEntity<ApiResponseDto<List<ItemResponseDto>>> getPendingItems() {
        return ResponseEntity.ok(ApiResponseDto.ok(itemService.getPendingItems()));
    }

    @Operation(
            summary = "Approve a pending item (admin)",
            description = "Admin approves a carrier-submitted item. " +
                          "Once approved the item becomes visible in the catalogue and can be used in offers."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item approved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Item is already approved"),
            @ApiResponse(responseCode = "404", description = "Item not found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PatchMapping("/{itemId}/approve")
    public ResponseEntity<ApiResponseDto<ItemResponseDto>> approveItem(
            @Parameter(description = "ID of the item to approve") @PathVariable UUID itemId) {
        return ResponseEntity.ok(ApiResponseDto.ok(itemService.approveItem(itemId)));
    }

    @Operation(
            summary = "Reject a pending item (admin)",
            description = "Admin rejects a carrier-submitted item. " +
                          "The item is deactivated and will not appear anywhere."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item rejected and deactivated"),
            @ApiResponse(responseCode = "400", description = "Item is already approved"),
            @ApiResponse(responseCode = "404", description = "Item not found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @DeleteMapping("/{itemId}/reject")
    public ResponseEntity<Void> rejectItem(
            @Parameter(description = "ID of the item to reject") @PathVariable UUID itemId) {
        itemService.rejectItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
