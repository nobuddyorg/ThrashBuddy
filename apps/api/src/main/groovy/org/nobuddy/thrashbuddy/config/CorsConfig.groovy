package org.nobuddy.thrashbuddy.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig implements WebMvcConfigurer {

    @Value('${ALLOWED_ORIGINS:http://localhost:4200}')
    String allowedOrigins

    @Override
    void addCorsMappings(CorsRegistry registry) {
        def origins = allowedOrigins.split(',')*.trim().findAll { it }
        if (origins) {
            registry.addMapping("/api/**").allowedOrigins(origins as String[])
        }
    }
}
