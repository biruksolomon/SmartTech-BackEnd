package com.smarttech.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Value("\n\n\t Admin credentials(The Only Admin!) \n\n\t\tUsername: Admin@Test.com \n\n\t\tPassword: AdminTest@123")
    private String AdminCredentials;

    @Value("\n\n\t Buyer credentials(Optional, Also Register and login) \n\n\t\tUsername: User@Test.com \n\n\t\tPassword: UserTest@123")
    private String BuyerCredentials;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Tech E-commerce & Maintenance API")
                        .description("API for Smart Tech computer retail and maintenance platform \n\n Sentayehu Abebe Computer Retail Trade and Maintenance"+AdminCredentials+BuyerCredentials)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Smart Tech Support")
                                .email("support@smarttech.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
