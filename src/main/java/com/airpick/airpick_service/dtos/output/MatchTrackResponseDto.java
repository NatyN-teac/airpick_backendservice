package com.airpick.airpick_service.dtos.output;

import java.util.List;

/**
 * Delivery tracking summary grouped by lifecycle stage.
 * <ul>
 *   <li>{@code collected} — match ACCEPTED (picked up / ready to start delivery)</li>
 *   <li>{@code inProgress} — match IN_PROGRESS (carrier en route)</li>
 *   <li>{@code completed} — match COMPLETED (delivered)</li>
 * </ul>
 * PENDING, CANCELLED, and REJECTED matches are excluded.
 */
public record MatchTrackResponseDto(
        List<MatchResponseDto> collected,
        List<MatchResponseDto> inProgress,
        List<MatchResponseDto> completed
) {}
