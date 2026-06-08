package com.airpick.airpick_service.commons.configs;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GcsConfig {

    private final GcsProperties gcsProperties;

    @Bean
    public Storage gcsStorage() {
        if (!gcsProperties.isConfigured()) {
            log.warn("gcp.storage.bucket-name not configured — pickup photo uploads will fail until set");
        } else {
            log.info("GCS Storage client via ADC, bucket: {}", gcsProperties.getBucketName());
            if (gcsProperties.isProfileBucketConfigured()) {
                log.info("GCS profile bucket: {}", gcsProperties.getProfileBucketName());
            }
        }
        return StorageOptions.getDefaultInstance().getService();
    }
}
