package com.company.leavemanagementsystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for the Mini Leave Management System.
 * Provides comprehensive API documentation with detailed information about
 * endpoints,
 * request/response models, and examples.
 */
@Configuration
@EnableConfigurationProperties(LeaveManagementProperties.class)
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mini Leave Management System API")
                        .version("1.0.0")
                        .description("""
                                A comprehensive leave management system API for small to medium-sized organizations.

                                ## Features
                                - Employee management with hierarchical relationships
                                - Leave application with multiple types and durations
                                - Automated approval workflows with delegation
                                - Real-time leave balance tracking
                                - Emergency and backdated leave support
                                - Comprehensive audit trails

                                ## Business Rules
                                - Working days: Monday to Friday (weekends excluded)
                                - Leave year: Calendar year (January to December)
                                - Emergency leave: Auto-approved up to 2 days for same-day requests
                                - Backdated requests: Allowed up to 30 days in the past
                                - Half-day leaves: Count as 0.5 days against balance

                                ## Authentication
                                Currently using basic authentication. Future versions will support JWT/OAuth2.
                                """)
                        .contact(new Contact()
                                .name("Leave Management System Support")
                                .email("support@company.com")
                                .url("https://github.com/company/mini-leave-management-system"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.company.com")
                                .description("Production Server")));
    }
}