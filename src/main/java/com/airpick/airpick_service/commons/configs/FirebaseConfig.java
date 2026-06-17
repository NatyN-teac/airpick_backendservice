// package com.airpick.airpick_service.commons.configs;

// import com.google.auth.oauth2.GoogleCredentials;
// import com.google.firebase.FirebaseApp;
// import com.google.firebase.FirebaseOptions;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.core.io.ClassPathResource;
// import org.springframework.core.io.FileSystemResource;
// import org.springframework.core.io.Resource;

// import java.io.IOException;
// import java.io.InputStream;

// /**
//  * Initialises Firebase Admin SDK.
//  * <p>
//  * {@code firebase.service-account-path} is resolved first as a filesystem path (absolute or
//  * relative to the working directory). If the file does not exist there, it falls back to the
//  * classpath — so {@code src/main/resources/your-key.json} works both when running from source
//  * and when running from a packaged JAR that bundles the file.
//  */
// @Slf4j
// @Configuration
// public class FirebaseConfig {

//     @Value("${firebase.service-account-path}")
//     private String serviceAccountPath;

//     @Bean
//     public FirebaseApp firebaseApp() throws IOException {
//         // if (!FirebaseApp.getApps().isEmpty()) {
//         //     log.info("FirebaseApp already initialised — reusing existing instance");
//         //     return FirebaseApp.getInstance();
//         // }

//         // Resource resource = resolveResource(serviceAccountPath);
//         // log.info("Initialising Firebase Admin SDK from: {}", resource.getDescription());

//         // try (InputStream stream = resource.getInputStream()) {
//         //     FirebaseOptions options = FirebaseOptions.builder()
//         //             .setCredentials(GoogleCredentials.fromStream(stream))
//         //             .build();
//         //     return FirebaseApp.initializeApp(options);
//         // }
//     }

//     /**
//      * Resolves the credential file as a filesystem path first (allows absolute paths and
//      * env-var overrides), then falls back to classpath for bundled resources.
//      */
//     private Resource resolveResource(String path) {
//         FileSystemResource fsResource = new FileSystemResource(path);
//         if (fsResource.exists()) {
//             return fsResource;
//         }
//         // Strip leading "src/main/resources/" if present — not valid as a classpath prefix
//         String classpathPath = path.replaceFirst("^src/main/resources/", "");
//         return new ClassPathResource(classpathPath);
//     }
// }
