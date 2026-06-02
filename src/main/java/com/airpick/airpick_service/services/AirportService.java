package com.airpick.airpick_service.services;

import com.airpick.airpick_service.dtos.input.CreateAirportRequestDto;
import com.airpick.airpick_service.dtos.input.UpdateAirportRequestDto;
import com.airpick.airpick_service.dtos.output.AirportResponseDto;
import com.airpick.airpick_service.models.Airport;
import com.airpick.airpick_service.repositories.AirportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for admin-controlled airport management.
 * <p>
 * Airports are referenced by {@link com.airpick.airpick_service.models.FlightLeg}
 * and must be active to be selectable when creating a flight.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AirportService {

    private final AirportRepository airportRepository;

    /**
     * Creates a new airport.
     *
     * @param request airport details — name, IATA code, country, city
     * @return the saved airport as a response DTO
     * @throws IllegalArgumentException if the IATA code is already in use
     */
    @Transactional
    public AirportResponseDto createAirport(CreateAirportRequestDto request) {
        log.info("Creating airport with code: {}", request.code());

        airportRepository.findActiveByCode(request.code()).ifPresent(existing -> {
            log.warn("Airport code already exists: {}", request.code());
            throw new IllegalArgumentException("Airport code already in use: " + request.code());
        });

        Airport airport = Airport.builder()
                .name(request.name())
                .code(request.code().toUpperCase())
                .country(request.country())
                .city(request.city())
                .isActive(true)
                .build();

        Airport saved = airportRepository.save(airport);
        log.info("Airport created with id: {}", saved.getId());

        return AirportResponseDto.from(saved);
    }

    /**
     * Updates an existing airport's details.
     * <p>
     * Only non-null fields in the request are applied.
     *
     * @param id      the ID of the airport to update
     * @param request fields to update; null values are ignored
     * @return the updated airport as a response DTO
     * @throws IllegalArgumentException if the airport is not found or inactive
     */
    @Transactional
    public AirportResponseDto updateAirport(UUID id, UpdateAirportRequestDto request) {
        log.info("Updating airport id: {}", id);

        Airport airport = airportRepository.findActiveById(id)
                .orElseThrow(() -> {
                    log.warn("Airport not found or inactive: {}", id);
                    return new IllegalArgumentException("Airport not found: " + id);
                });

        if (request.name()    != null) airport.setName(request.name());
        if (request.code()    != null) airport.setCode(request.code().toUpperCase());
        if (request.country() != null) airport.setCountry(request.country());
        if (request.city()    != null) airport.setCity(request.city());

        Airport saved = airportRepository.save(airport);
        log.info("Airport updated id: {}", saved.getId());

        return AirportResponseDto.from(saved);
    }

    /**
     * Soft-deletes an airport by marking it as inactive.
     * <p>
     * The record is preserved in the database for historical flight leg references.
     *
     * @param id the ID of the airport to deactivate
     * @throws IllegalArgumentException if the airport is not found or already inactive
     */
    @Transactional
    public void deleteAirport(UUID id) {
        log.info("Soft-deleting airport id: {}", id);

        Airport airport = airportRepository.findActiveById(id)
                .orElseThrow(() -> {
                    log.warn("Airport not found or already inactive: {}", id);
                    return new IllegalArgumentException("Airport not found: " + id);
                });

        airport.setActive(false);
        airportRepository.save(airport);
        log.info("Airport deactivated id: {}", id);
    }

    /**
     * Returns all active airports available for selection when creating a flight.
     *
     * @return list of active airports ordered by name
     */
    public List<AirportResponseDto> getAllAirports() {
        return airportRepository.findAllActive()
                .stream()
                .map(AirportResponseDto::from)
                .toList();
    }
}
