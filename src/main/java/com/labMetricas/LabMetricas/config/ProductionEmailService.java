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
public class ProductionEmailService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionEmailService.class);

    @Autowired
    private Resend resend;

    @Value("${RESEND_DEFAULT_SENDER:${RESEND_DEFAULT_SENDE:${resend.default.sender:}}}")
    private String defaultSender;

    @Value("${RESEND_API_KEY:${resend.api.key:}}")
    private String apiKey;

    private final List<String> verifiedDomains;

    public ProductionEmailService(@Value("${RESEND_VERIFIED_DOMAINS:${resend.verified.domains:}}") String verifiedDomainsCsv) {
        if (verifiedDomainsCsv == null || verifiedDomainsCsv.trim().isEmpty()) {
            this.verifiedDomains = Collections.emptyList();
        } else {
            this.verifiedDomains = Arrays.stream(verifiedDomainsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        }
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

    /**
     * Enviar email de bienvenida a un nuevo usuario
     */
    public boolean sendWelcomeEmail(String to, String userName) {
        logger.info("Welcome email suppressed (to: {})", to);
        return true;
    }

    /**
     * Enviar email de notificación de mantenimiento
     */
    public boolean sendMaintenanceNotification(String to, String userName, String maintenanceType, String scheduledDate) {
        logger.info("Maintenance notification email suppressed (to: {})", to);
        return true;
    }

    /**
     * Enviar email de recordatorio de contraseña
     */
    public boolean sendPasswordReminder(String to, String userName) {
        logger.info("Password reminder email suppressed (to: {})", to);
        return true;
    }

    /**
     * Enviar email de confirmación de acción
     */
    public boolean sendActionConfirmation(String to, String userName, String action, String details) {
        logger.info("Action confirmation email suppressed for action: {} (to: {})", action, to);
        return true;
    }

    /**
     * Enviar email personalizado
     */
    public boolean sendCustomEmail(String to, String subject, String htmlContent) {
        String normalized = subject == null ? "" : subject.trim().toLowerCase();
        boolean allowed =
            normalized.contains("tus credenciales de acceso") ||
            normalized.contains("restablecer contraseña") ||
            normalized.contains("contraseña actualizada");

        if (!allowed) {
            logger.info("Custom email suppressed (to: {}, subject: {})", to, subject);
            return true;
        }

        return sendEmail(to, subject, htmlContent);
    }

    private boolean sendEmail(String to, String subject, String htmlContent) {
        try {
            logger.info("Sending production email to: {}", to);

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

            logger.info("Using sender: {}", defaultSender);
            
            SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                .from(defaultSender)
                .to(to)
                .subject(subject)
                .html(htmlContent)
                .build();

            SendEmailResponse response = resend.emails().send(sendEmailRequest);
            
            logger.info("Production email sent successfully to: {} with ID: {}", to, response.getId());
            return true;
        } catch (Exception e) {
            logger.error("Failed to send production email to: {}", to, e);
            return false;
        }
    }

    private String buildWelcomeEmailBody(String userName) {
        return """
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #007bff; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f8f9fa; padding: 20px; border-radius: 0 0 5px 5px; }
                    .welcome { color: #28a745; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>LabMetricas</h1>
                    </div>
                    <div class="content">
                        <h2 class="welcome">¡Bienvenido, %s!</h2>
                        <p>Nos complace darte la bienvenida a LabMetricas, tu plataforma integral para la gestión de métricas y mantenimiento.</p>
                        <p>Con tu cuenta podrás:</p>
                        <ul>
                            <li>Gestionar equipos y mantenimientos</li>
                            <li>Monitorear métricas en tiempo real</li>
                            <li>Recibir notificaciones importantes</li>
                            <li>Acceder a reportes detallados</li>
                        </ul>
                        <p>Si tienes alguna pregunta, no dudes en contactarnos.</p>
                        <p><em>¡Gracias por elegir LabMetricas!</em></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName);
    }

    private String buildMaintenanceNotificationBody(String userName, String maintenanceType, String scheduledDate) {
        return """
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #ffc107; color: #333; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f8f9fa; padding: 20px; border-radius: 0 0 5px 5px; }
                    .important { color: #dc3545; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Notificación de Mantenimiento</h1>
                    </div>
                    <div class="content">
                        <h2>Hola, %s</h2>
                        <p>Te informamos que se ha programado un mantenimiento en el sistema.</p>
                        <p><strong>Tipo de Mantenimiento:</strong> %s</p>
                        <p><strong>Fecha Programada:</strong> %s</p>
                        <p class="important">Por favor, asegúrate de guardar tu trabajo antes de la fecha indicada.</p>
                        <p>Te notificaremos cuando el mantenimiento esté completo.</p>
                        <p>Gracias por tu comprensión.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, maintenanceType, scheduledDate);
    }

    private String buildPasswordReminderBody(String userName) {
        return """
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #6c757d; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f8f9fa; padding: 20px; border-radius: 0 0 5px 5px; }
                    .reminder { color: #17a2b8; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Recordatorio de Contraseña</h1>
                    </div>
                    <div class="content">
                        <h2>Hola, %s</h2>
                        <p>Este es un recordatorio amigable para que actualices tu contraseña regularmente.</p>
                        <p class="reminder">Recomendaciones de seguridad:</p>
                        <ul>
                            <li>Usa contraseñas únicas y complejas</li>
                            <li>Cambia tu contraseña cada 3-6 meses</li>
                            <li>No compartas tu contraseña con nadie</li>
                            <li>Usa autenticación de dos factores si está disponible</li>
                        </ul>
                        <p>Si no solicitaste este recordatorio, puedes ignorarlo.</p>
                        <p>¡Mantén tu cuenta segura!</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName);
    }

    private String buildActionConfirmationBody(String userName, String action, String details) {
        return """
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #28a745; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f8f9fa; padding: 20px; border-radius: 0 0 5px 5px; }
                    .success { color: #28a745; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Confirmación de Acción</h1>
                    </div>
                    <div class="content">
                        <h2>Hola, %s</h2>
                        <p class="success">Tu acción ha sido procesada exitosamente.</p>
                        <p><strong>Acción realizada:</strong> %s</p>
                        <p><strong>Detalles:</strong> %s</p>
                        <p>Si no reconoces esta acción, por favor contacta al soporte técnico inmediatamente.</p>
                        <p>Gracias por usar LabMetricas.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, action, details);
    }
}
