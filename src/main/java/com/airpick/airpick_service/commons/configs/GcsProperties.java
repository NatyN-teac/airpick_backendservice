package com.airpick.airpick_service.commons.configs;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * GCS configuration — property keys aligned with projalpha convention.
 * Authentication uses Application Default Credentials (ADC); no JSON key file required on GCP.
 */
@Configuration
@Getter
public class GcsProperties {

    @Value("${gcp.storage.bucket-name:}")
    private String bucketName;

    @Value("${gcp.store.bucket-profile-name:}")
    private String profileBucketName;

    /** Signed read/upload URL lifetime for private objects. */
    @Value("${gcp.storage.signed-url-duration-minutes:15}")
    private int signedUrlDurationMinutes;

    public boolean isConfigured() {
        return bucketName != null && !bucketName.isBlank();
    }

    public boolean isProfileBucketConfigured() {
        return profileBucketName != null && !profileBucketName.isBlank();
    }
}
