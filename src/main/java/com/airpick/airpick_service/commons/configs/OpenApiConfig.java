package com.airpick.airpick_service.commons.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration for the Airpick Service API.
 * <p>
 * Defines a global JWT Bearer security scheme so that all protected endpoints
 * display the padlock icon in Swagger UI. Open endpoints override this globally
 * applied requirement at the operation level using {@code security = {}}.
 * <p>
 * Swagger UI is available at {@code /swagger-ui/index.html} in dev only.
 * It is disabled in the prod profile via application-prod.properties.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Airpick Service API")
                        .description("""
                                Backend API for the Airpick mobile application.

                                Authentication is Firebase-based: the mobile client authenticates \
                                with Firebase and passes the resulting ID token to the relevant \
                                endpoints. The server verifies the token, issues a signed JWT, \
                                and returns it in the response. All subsequent requests must \
                                include that JWT as a Bearer token.

                                Endpoints marked with an open padlock do not require authentication.
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Airpick Team")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT token returned by /users/register")
                        )
                );
    }
}
