package com.airpick.airpick_service.commons.configs;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Initialises the GCS {@link Storage} client.
 * <p>
 * When {@code gcp.storage.credentials-path} is set (local dev) the client is built with explicit
 * {@link ServiceAccountCredentials} so that V4 signed URL generation works without the
 * GOOGLE_APPLICATION_CREDENTIALS environment variable.
 * <p>
 * When the path is empty (production on GCP) ADC is used; the attached service account must have
 * {@code roles/iam.serviceAccountTokenCreator} on itself for signed URL generation.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GcsConfig {

    private final GcsProperties gcsProperties;

    @Bean
    public Storage gcsStorage() throws IOException {
        if (!gcsProperties.isConfigured()) {
            log.warn("gcp.storage.bucket-name not configured — GCS uploads will fail until set");
        }

        if (gcsProperties.hasExplicitCredentials()) {
            log.info("GCS Storage client using explicit service account credentials from: {}",
                    gcsProperties.getCredentialsPath());
            try (FileInputStream credStream = new FileInputStream(gcsProperties.getCredentialsPath())) {
                ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(credStream);
                return StorageOptions.newBuilder()
                        .setCredentials(credentials)
                        .build()
                        .getService();
            }
        }

        log.info("GCS Storage client via ADC (production mode), bucket: {}", gcsProperties.getBucketName());
        return StorageOptions.getDefaultInstance().getService();
    }
}
