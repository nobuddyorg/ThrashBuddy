package org.nobuddy.thrashbuddy.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig implements WebMvcConfigurer {

    // Port-matching is intentionally not used here: the app is reached through
    // nginx-ingress (and, in local/CI setups, kubectl port-forward), which can
    // remap the client-facing port to something the origin server never sees.
    // Matching by host only (wildcard port) is what actually reflects "same
    // logical origin" in that topology, while still rejecting other hosts.
    @Value('${ALLOWED_ORIGIN_PATTERNS:http://localhost:*}')
    String allowedOriginPatterns

    @Override
    void addCorsMappings(CorsRegistry registry) {
        def patterns = allowedOriginPatterns.split(',')*.trim().findAll { it }
        if (patterns) {
            // Spring only allows GET/HEAD/POST by default when allowedMethods isn't
            // set explicitly - the API also uses DELETE (/api/delete), which would
            // otherwise be rejected with a CORS error even for an allowed origin.
            registry.addMapping("/api/**")
                    .allowedOriginPatterns(patterns as String[])
                    .allowedMethods("GET", "POST", "DELETE")
        }
    }
}
