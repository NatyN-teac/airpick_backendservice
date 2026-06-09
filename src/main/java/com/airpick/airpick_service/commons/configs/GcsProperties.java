package com.airpick.airpick_service.commons.configs;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * GCS configuration.
 * <p>
 * On GCP (Cloud Run / GCE) leave {@code gcp.storage.credentials-path} empty — ADC supplies the
 * attached service account automatically and signed URLs work if the SA has
 * {@code roles/iam.serviceAccountTokenCreator} on itself.
 * <p>
 * For local development set {@code gcp.storage.credentials-path} to the same service account
 * JSON used by Firebase (or any SA with Storage Object Admin + Token Creator roles).
 * This lets the GCS client derive a signing key without needing the GOOGLE_APPLICATION_CREDENTIALS
 * env var.
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

    /**
     * Optional path to a service account JSON key file used ONLY for generating V4 signed URLs.
     * Leave blank on GCP — ADC is used instead (requires SA to have Token Creator role on itself).
     * Set to your Firebase service account JSON path for local development.
     */
    @Value("${gcp.storage.credentials-path:}")
    private String credentialsPath;

    public boolean isConfigured() {
        return bucketName != null && !bucketName.isBlank();
    }

    public boolean isProfileBucketConfigured() {
        return profileBucketName != null && !profileBucketName.isBlank();
    }

    public boolean hasExplicitCredentials() {
        return credentialsPath != null && !credentialsPath.isBlank();
    }
}
