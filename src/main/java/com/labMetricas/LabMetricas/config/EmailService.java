package com.labMetricas.LabMetricas.config;

import com.resend.Resend;
import com.resend.services.emails.model.SendEmailRequest;
import com.resend.services.emails.model.SendEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private Resend resend;

    @Value("${RESEND_DEFAULT_SENDER:${RESEND_DEFAULT_SENDE:${resend.default.sender:}}}")
    private String defaultSender;

    @Value("${RESEND_API_KEY:${resend.api.key:}}")
    private String apiKey;

    // Lista de dominios verificados
    private final List<String> verifiedDomains;

    public EmailService(@Value("${RESEND_VERIFIED_DOMAINS:${resend.verified.domains:}}") String verifiedDomainsCsv) {
        if (verifiedDomainsCsv == null || verifiedDomainsCsv.trim().isEmpty()) {
            this.verifiedDomains = Collections.emptyList();
        } else {
            this.verifiedDomains = Arrays.stream(verifiedDomainsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        }
    }

    public boolean sendEmail(String to, String subject, String htmlContent) {
        return sendEmail(defaultSender, to, subject, htmlContent);
    }

    public boolean sendEmail(String from, String to, String subject, String htmlContent) {
        try {
            logger.info("Attempting to send email to: {}", to);
            logger.debug("Email details - From: {}, Subject: {}, API Key configured: {}", 
                from, subject, apiKey != null && !apiKey.isEmpty());

            if (defaultSender == null || defaultSender.isBlank()) {
                logger.error("RESEND_DEFAULT_SENDER is not configured (defaultSender is blank). Email will not be sent.");
                return false;
            }

            if (!isFromVerifiedDomain(defaultSender)) {
                logger.error(
                    "RESEND_DEFAULT_SENDER domain is not verified (sender: '{}', verified domains: {}). Update RESEND_DEFAULT_SENDER to use one of the verified domains or verify the domain in Resend.",
                    defaultSender,
                    verifiedDomains
                );
                return false;
            }

            // Validar que el remitente sea de un dominio verificado
            if (!isFromVerifiedDomain(from)) {
                logger.warn("Sender domain not verified, using default sender: {}", defaultSender);
                from = defaultSender;
            }

            SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .html(htmlContent)
                .build();

            SendEmailResponse response = resend.emails().send(sendEmailRequest);
            
            logger.info("Email sent successfully to: {} with ID: {}", to, response.getId());
            return true;
        } catch (Exception e) {
            logger.error("Failed to send email to: {}", to, e);
            return false;
        }
    }

    public boolean sendTestEmail(String to) {
        String subject = "Test Email from LabMetricas";
        String htmlContent = buildTestEmailBody();
        return sendEmail(to, subject, htmlContent);
    }

    public boolean sendTestEmailWithCustomSender(String to, String customSender) {
        String subject = "Test Email from LabMetricas (Custom Sender)";
        String htmlContent = buildTestEmailBody();
        return sendEmail(customSender, to, subject, htmlContent);
    }

    private boolean isFromVerifiedDomain(String from) {
        if (from == null || from.isBlank()) return false;

        String fromDomain = extractDomain(from);
        if (fromDomain == null || fromDomain.isBlank()) return false;

        if (verifiedDomains.isEmpty()) {
            String defaultDomain = extractDomain(defaultSender);
            if (defaultDomain == null || defaultDomain.isEmpty()) return false;
            return defaultDomain.equalsIgnoreCase(fromDomain);
        }

        return verifiedDomains.stream().anyMatch(verified -> fromDomain.equalsIgnoreCase(verified));
    }

    private String extractDomain(String from) {
        if (from == null) return null;
        String value = from.trim();
        if (value.isEmpty()) return null;

        int lt = value.lastIndexOf('<');
        int gt = value.lastIndexOf('>');
        if (lt >= 0 && gt > lt) {
            value = value.substring(lt + 1, gt).trim();
        }

        int at = value.lastIndexOf('@');
        if (at < 0 || at == value.length() - 1) return null;

        return value.substring(at + 1).trim();
    }

    public List<String> getVerifiedDomains() {
        return verifiedDomains;
    }

    public String getDefaultSender() {
        return defaultSender;
    }

    private String buildTestEmailBody() {
        return """
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #007bff; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f8f9fa; padding: 20px; border-radius: 0 0 5px 5px; }
                    .success { color: #28a745; font-weight: bold; }
                    .info { background-color: #e7f3ff; padding: 15px; border-radius: 5px; margin: 15px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>LabMetricas - Test Email</h1>
                    </div>
                    <div class="content">
                        <h2>¡Email de prueba exitoso!</h2>
                        <p>Este es un email de prueba para verificar que el servicio de correo electrónico está funcionando correctamente.</p>
                        <p class="success">✅ El servicio de Resend está configurado y funcionando.</p>
                        <div class="info">
                            <p><strong>Remitente:</strong> %s</p>
                            <p><strong>Destinatario:</strong> %s</p>
                            <p><strong>Fecha y hora:</strong> %s</p>
                        </div>
                        <p>Si recibes este email, significa que tu configuración de Resend está correcta.</p>
                        <p><em>Este es un email de prueba automático. No es necesario responder.</em></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(defaultSender, "{{TO_EMAIL}}", java.time.LocalDateTime.now());
    }
}
