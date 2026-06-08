package com.airpick.airpick_service.services;

import com.airpick.airpick_service.dtos.output.EngagementResponseDto;
import com.airpick.airpick_service.dtos.output.MatchResponseDto;
import com.airpick.airpick_service.dtos.output.OfferProposalResponseDto;
import com.airpick.airpick_service.models.MatchStatus;
import com.airpick.airpick_service.models.ModeType;
import com.airpick.airpick_service.models.OfferProposalStatus;
import com.airpick.airpick_service.models.User;
import com.airpick.airpick_service.repositories.MatchRepository;
import com.airpick.airpick_service.repositories.OfferProposalRepository;
import com.airpick.airpick_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EngagementService {

    private static final int MAX_ITEMS = 5;
    private static final List<MatchStatus> ACTIVE_MATCH_STATUSES = List.of(
            MatchStatus.PENDING,
            MatchStatus.ACCEPTED,
            MatchStatus.IN_PROGRESS
    );

    private final UserRepository userRepository;
    private final OfferProposalRepository offerProposalRepository;
    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public EngagementResponseDto getEngagement(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        if (user.getActiveMode() == null) {
            throw new IllegalArgumentException("Mode not set for user");
        }

        ModeType mode = ModeType.valueOf(user.getActiveMode().getName());
        log.info("Fetching engagement for user {} in {} mode", user.getId(), mode);

        return switch (mode) {
            case CARRIER -> buildCarrierEngagement(user, mode);
            case SHIPPER -> buildShipperEngagement(user, mode);
        };
    }

    private EngagementResponseDto buildCarrierEngagement(User user, ModeType mode) {
        List<OfferProposalResponseDto> proposalsSent = offerProposalRepository
                .findTop5ByCarrierIdAndStatusOrderByUpdatedAtDesc(
                        user.getId(),
                        OfferProposalStatus.PENDING,
                        PageRequest.of(0, MAX_ITEMS))
                .stream()
                .map(OfferProposalResponseDto::from)
                .toList();

        List<MatchResponseDto> matchedOffers = matchRepository
                .findTop5ByCarrierIdAndStatusInOrderByUpdatedAtDesc(
                        user.getId(),
                        ACTIVE_MATCH_STATUSES,
                        PageRequest.of(0, MAX_ITEMS))
                .stream()
                .map(MatchResponseDto::from)
                .toList();

        return new EngagementResponseDto(
                mode.name(),
                proposalsSent,
                List.of(),
                matchedOffers
        );
    }

    private EngagementResponseDto buildShipperEngagement(User user, ModeType mode) {
        List<OfferProposalResponseDto> proposalsReceived = offerProposalRepository
                .findTop5ByOfferRequest_Shipper_IdAndStatusOrderByUpdatedAtDesc(
                        user.getId(),
                        OfferProposalStatus.PENDING,
                        PageRequest.of(0, MAX_ITEMS))
                .stream()
                .map(OfferProposalResponseDto::from)
                .toList();

        List<MatchResponseDto> matchedOffers = matchRepository
                .findTop5ByShipperIdAndStatusInOrderByUpdatedAtDesc(
                        user.getId(),
                        ACTIVE_MATCH_STATUSES,
                        PageRequest.of(0, MAX_ITEMS))
                .stream()
                .map(MatchResponseDto::from)
                .toList();

        return new EngagementResponseDto(
                mode.name(),
                List.of(),
                proposalsReceived,
                matchedOffers
        );
    }
}
