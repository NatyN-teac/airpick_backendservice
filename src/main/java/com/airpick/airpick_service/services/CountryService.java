package com.airpick.airpick_service.services;

import com.airpick.airpick_service.dtos.input.CreateCountryRequestDto;
import com.airpick.airpick_service.dtos.output.CountryResponseDto;
import com.airpick.airpick_service.models.Country;
import com.airpick.airpick_service.repositories.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    public List<CountryResponseDto> getAllCountries() {
        return countryRepository.findAllByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(CountryResponseDto::from)
                .toList();
    }

    @Transactional
    public CountryResponseDto createCountry(CreateCountryRequestDto dto) {
        if (countryRepository.existsByNameIgnoreCase(dto.name())) {
            throw new IllegalArgumentException("Country already exists: " + dto.name());
        }
        if (countryRepository.existsByCountryCodeIgnoreCase(dto.countryCode())) {
            throw new IllegalArgumentException("Country code already in use: " + dto.countryCode());
        }

        Country country = Country.builder()
                .name(dto.name())
                .countryCode(dto.countryCode().toUpperCase())
                .isActive(true)
                .build();

        Country saved = countryRepository.save(country);
        log.info("Country created: {} ({})", saved.getName(), saved.getCountryCode());
        return CountryResponseDto.from(saved);
    }
}
