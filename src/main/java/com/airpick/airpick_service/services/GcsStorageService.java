package com.airpick.airpick_service.services;

import com.airpick.airpick_service.commons.configs.GcsProperties;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GcsStorageService {

    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final Storage gcsStorage;
    private final GcsProperties gcsProperties;

    /**
     * Streams a pickup proof photo into the private bucket at
     * {@code matches/{matchId}/pickup-photo.{ext}}.
     */
    public String uploadMatchPickupPhoto(UUID matchId, InputStream content, String contentType, long size)
            throws IOException {
        requireConfigured();

        String extension = extensionFor(contentType);
        String objectPath = "matches/" + matchId + "/pickup-photo." + extension;

        BlobInfo blobInfo = BlobInfo.newBuilder(gcsProperties.getBucketName(), objectPath)
                .setContentType(contentType)
                .build();

        gcsStorage.createFrom(blobInfo, content);
        log.info("Uploaded pickup photo for match {} to gs://{}/{} ({} bytes)",
                matchId, gcsProperties.getBucketName(), objectPath, size);
        return objectPath;
    }

    public void deleteObject(String objectPath) {
        if (objectPath == null || gcsStorage == null || !gcsProperties.isConfigured()) {
            return;
        }
        boolean deleted = gcsStorage.delete(BlobId.of(gcsProperties.getBucketName(), objectPath));
        if (deleted) {
            log.info("Deleted GCS object: {}", objectPath);
        }
    }

    public SignedUrlResult generateSignedReadUrl(String objectPath) {
        requireConfigured();

        Blob blob = gcsStorage.get(BlobId.of(gcsProperties.getBucketName(), objectPath));
        if (blob == null || !blob.exists()) {
            throw new IllegalArgumentException("Pickup photo not found in storage");
        }

        int durationMinutes = gcsProperties.getSignedUrlDurationMinutes();
        BlobInfo blobInfo = BlobInfo.newBuilder(gcsProperties.getBucketName(), objectPath).build();
        URL url = gcsStorage.signUrl(
                blobInfo,
                durationMinutes,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature());

        LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(durationMinutes);
        return new SignedUrlResult(url.toString(), expiresAt);
    }

    public static String extensionFor(String contentType) {
        String normalized = contentType == null ? "" : contentType.toLowerCase().trim();
        String extension = CONTENT_TYPE_EXTENSIONS.get(normalized);
        if (extension == null) {
            throw new IllegalArgumentException(
                    "Unsupported image type: " + contentType + ". Allowed: JPEG, PNG, WebP");
        }
        return extension;
    }

    public static boolean isAllowedImageType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return CONTENT_TYPE_EXTENSIONS.containsKey(contentType.toLowerCase().trim());
    }

    private void requireConfigured() {
        if (gcsStorage == null || !gcsProperties.isConfigured()) {
            throw new IllegalStateException("Cloud storage is not configured");
        }
    }

    public record SignedUrlResult(String signedUrl, LocalDateTime expiresAt) {}
}
