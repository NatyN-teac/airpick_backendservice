package com.airpick.airpick_service.services;

import com.airpick.airpick_service.dtos.input.CreateFlightRequestDto;
import com.airpick.airpick_service.dtos.input.UpdateFlightRequestDto;
import com.airpick.airpick_service.dtos.output.FlightResponseDto;
import com.airpick.airpick_service.models.*;
import com.airpick.airpick_service.repositories.AirportRepository;
import com.airpick.airpick_service.repositories.FlightRepository;
import com.airpick.airpick_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for flight creation, updates, soft deletion, and retrieval.
 * <p>
 * The authenticated user is resolved from their email (extracted from the JWT).
 * Ownership is verified before any mutation operation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;
    private final UserRepository userRepository;
    private final AirportRepository airportRepository;

    /**
     * Creates a new flight with one or more legs for the authenticated user.
     * <p>
     * Exactly one leg is required for {@link FlightType#ONE_WAY};
     * exactly two legs are required for {@link FlightType#ROUND_TRIP}.
     *
     * @param email   the authenticated user's email resolved from the JWT
     * @param request flight type and ordered list of leg details
     * @return the saved flight as a response DTO
     * @throws IllegalArgumentException if the user is not found, any airport is not found,
     *                                  or the leg count does not match the flight type
     */
    @Transactional
    public FlightResponseDto createFlight(String email, CreateFlightRequestDto request) {
        log.info("Creating {} flight for user: {}", request.flightType(), email);

        User user = resolveUser(email);
        validateLegCount(request.flightType(), request.legs().size());

        Flight flight = Flight.builder()
                .user(user)
                .flightType(request.flightType())
                .isBooked(false)
                .isDeleted(false)
                .legs(new ArrayList<>())
                .build();

        List<FlightLeg> legs = buildLegs(flight, request.legs());
        flight.getLegs().addAll(legs);

        Flight saved = flightRepository.save(flight);
        log.info("Flight created with id: {}", saved.getId());

        return FlightResponseDto.from(saved);
    }

    /**
     * Updates the legs of an existing flight owned by the authenticated user.
     * <p>
     * All existing legs are replaced with the provided list.
     *
     * @param flightId the ID of the flight to update
     * @param email    the authenticated user's email — must match the flight owner
     * @param request  new leg details; count must match the flight's type
     * @return the updated flight as a response DTO
     * @throws IllegalArgumentException if the flight is not found, the user is not the owner,
     *                                  or the leg count does not match the flight type
     */
    @Transactional
    public FlightResponseDto updateFlight(UUID flightId, String email, UpdateFlightRequestDto request) {
        log.info("Updating flight id: {} for user: {}", flightId, email);

        Flight flight = resolveOwnedFlight(flightId, email);
        validateLegCount(flight.getFlightType(), request.legs().size());

        flight.getLegs().clear();

        List<FlightLeg> updatedLegs = new ArrayList<>();
        for (int i = 0; i < request.legs().size(); i++) {
            UpdateFlightRequestDto.LegDto dto = request.legs().get(i);
            Airport src  = resolveAirport(dto.srcAirportId());
            Airport dest = resolveAirport(dto.destAirportId());

            updatedLegs.add(FlightLeg.builder()
                    .flight(flight)
                    .legOrder(i + 1)
                    .srcAirport(src)
                    .destAirport(dest)
                    .departureDate(dto.departureDate())
                    .departureTime(dto.departureTime())
                    .arrivalDate(dto.arrivalDate())
                    .arrivalTime(dto.arrivalTime())
                    .build());
        }

        flight.getLegs().addAll(updatedLegs);
        Flight saved = flightRepository.save(flight);
        log.info("Flight updated id: {}", saved.getId());

        return FlightResponseDto.from(saved);
    }

    /**
     * Soft-deletes a flight owned by the authenticated user.
     * <p>
     * Sets {@code isDeleted = true}. The record is preserved for audit purposes.
     *
     * @param flightId the ID of the flight to remove
     * @param email    the authenticated user's email — must match the flight owner
     * @throws IllegalArgumentException if the flight is not found or the user is not the owner
     */
    @Transactional
    public void removeFlight(UUID flightId, String email) {
        log.info("Soft-deleting flight id: {} for user: {}", flightId, email);

        Flight flight = resolveOwnedFlight(flightId, email);
        flight.setDeleted(true);
        flightRepository.save(flight);

        log.info("Flight soft-deleted id: {}", flightId);
    }

    /**
     * Returns all non-deleted flights across all users.
     * <p>
     * Intended for admin use — provides a full view of active flight records.
     *
     * @return list of all non-deleted flights ordered by creation date descending
     */
    public List<FlightResponseDto> getAllFlights() {
        log.info("Fetching all non-deleted flights (admin)");
        List<FlightResponseDto> flights = flightRepository.findAll()
                .stream()
                .map(FlightResponseDto::from)
                .toList();
        log.info("Returning {} flights", flights.size());
        return flights;
    }

    /**
     * Returns all non-deleted flights for the authenticated user.
     *
     * @param email the authenticated user's email resolved from the JWT
     * @return list of flights ordered by creation date descending
     */
    public List<FlightResponseDto> getAllFlightsByUsername(String email) {
        User user = resolveUser(email);
        return flightRepository.findAllByUserId(user.getId())
                .stream()
                .map(FlightResponseDto::from)
                .toList();
    }

    /**
     * Returns all non-deleted, non-booked flights for a given user.
     * <p>
     * Used to find flights still available for matching with a shipment.
     *
     * @param userId the internal ID of the user
     * @return list of available flights ordered by creation date descending
     */
    public List<FlightResponseDto> getAvailableFlightsByUserId(UUID userId) {
        return flightRepository.findAvailableByUserId(userId)
                .stream()
                .map(FlightResponseDto::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", email);
                    return new IllegalArgumentException("User not found: " + email);
                });
    }

    private Airport resolveAirport(UUID airportId) {
        return airportRepository.findActiveById(airportId)
                .orElseThrow(() -> {
                    log.warn("Airport not found or inactive: {}", airportId);
                    return new IllegalArgumentException("Airport not found: " + airportId);
                });
    }

    private Flight resolveOwnedFlight(UUID flightId, String email) {
        Flight flight = flightRepository.findActiveById(flightId)
                .orElseThrow(() -> {
                    log.warn("Flight not found: {}", flightId);
                    return new IllegalArgumentException("Flight not found: " + flightId);
                });

        if (!flight.getUser().getEmail().equals(email)) {
            log.warn("Ownership mismatch — flight {} does not belong to {}", flightId, email);
            throw new IllegalArgumentException("Flight not found: " + flightId);
        }

        return flight;
    }

    private void validateLegCount(FlightType type, int count) {
        int expected = type == FlightType.ONE_WAY ? 1 : 2;
        if (count != expected) {
            throw new IllegalArgumentException(
                    type.name() + " flight requires exactly " + expected + " leg(s), got " + count);
        }
    }

    private List<FlightLeg> buildLegs(Flight flight, List<CreateFlightRequestDto.LegDto> legDtos) {
        List<FlightLeg> legs = new ArrayList<>();
        for (int i = 0; i < legDtos.size(); i++) {
            CreateFlightRequestDto.LegDto dto = legDtos.get(i);
            Airport src  = resolveAirport(dto.srcAirportId());
            Airport dest = resolveAirport(dto.destAirportId());

            legs.add(FlightLeg.builder()
                    .flight(flight)
                    .legOrder(i + 1)
                    .srcAirport(src)
                    .destAirport(dest)
                    .departureDate(dto.departureDate())
                    .departureTime(dto.departureTime())
                    .arrivalDate(dto.arrivalDate())
                    .arrivalTime(dto.arrivalTime())
                    .build());
        }
        return legs;
    }
}
