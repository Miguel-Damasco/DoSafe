package com.miguel_damasco.DoSafe.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// @OpenAPIDefinition — sets the general metadata shown at the top of the Swagger UI page.
@OpenAPIDefinition(
    info = @Info(
        title = "DoSafe API",
        version = "1.0",
        description = "API for identity document processing. Extracts expiration dates from DNI, passport and driver's license using AWS Textract OCR."
    )
)
// @SecurityScheme — registers the JWT Bearer scheme so Swagger knows how to send the Authorization header.
// Once registered, endpoints annotated with @SecurityRequirement(name = "bearerAuth") will show a lock icon.
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT token obtained from POST /authentication/login. Paste the token without the 'Bearer' prefix."
)
@Configuration
public class OpenApiConfig {}
