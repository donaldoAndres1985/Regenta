package com.regenta.shared.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "regenta.cors")
public class PropiedadesCors {

    private List<String> allowedOriginPatterns = new ArrayList<>(List.of(
            "https://regenta.pages.dev",
            "https://*.regenta.pages.dev",
            "http://localhost:5000",
            "http://localhost:*"
    ));

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }
}
