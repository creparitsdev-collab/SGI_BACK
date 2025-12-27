package com.labMetricas.LabMetricas.config;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResendConfig {
    @Value("${resend.api.key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Bean
    public Resend resend() {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new IllegalStateException("Resend API key is not configured");
        }
        return new Resend(resendApiKey);
    }
}