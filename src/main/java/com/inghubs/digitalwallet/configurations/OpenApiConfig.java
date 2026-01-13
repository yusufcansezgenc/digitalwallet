package com.inghubs.digitalwallet.configurations;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Digital Wallet API for INGHubs",
        version = "1.0",
        description = "Documentation for Digital Wallet API for INGHubs",
        license = @License(name = "Apache 2.0", url = "https://spring.io")
    )
)
public class OpenApiConfig {
}
