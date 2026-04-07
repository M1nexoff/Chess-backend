package com.chessapp.server.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Chess Backend API",
                version = "1.0",
                description = "Mobile-first chess backend with real-time WebSocket gameplay, " +
                        "friends system, matchmaking, rating, and leaderboards.",
                contact = @Contact(name = "Chess App")
        ),
        servers = {
                @Server(url = "/", description = "Default Server")
        }
)
@SecurityScheme(
        name = "Bearer",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT token obtained from /api/auth/login"
)
public class OpenApiConfig {
    // Configuration is entirely annotation-driven
}
