package ru.itmo.alfa.comand4.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI clusterAnalysisOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ИИ-ассистент обработки обращений в техподдержку")
                        .description("REST API для обработки и анализа обращений в техподдержку")
                        .version("1.0.0")
                );
    }
}
