package com.labMetricas.LabMetricas.security;

import com.labMetricas.LabMetricas.passwordResetToken.model.PasswordResetToken;
import com.labMetricas.LabMetricas.passwordResetToken.repository.PasswordResetTokenRepository;
import com.labMetricas.LabMetricas.user.repository.UserRepository;
import com.labMetricas.LabMetricas.config.ProductionEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;
import com.labMetricas.LabMetricas.user.model.User;

@Service
public class PasswordResetService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProductionEmailService productionEmailService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Transactional
    public boolean initiatePasswordReset(String email) {
        try {
            String normalizedEmail = email == null ? null : email.trim().toLowerCase();
            if (normalizedEmail == null || normalizedEmail.isBlank()) {
                return false;
            }

            logger.info("Attempting password reset for email: {}", normalizedEmail);

            // First, check if the email exists in the database
            Optional<User> userOptional = userRepository.findByEmailNormalized(normalizedEmail);

            // Log additional details about the user lookup
            logger.info("User lookup result: {}", userOptional.isPresent() ? "User found" : "User not found");
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                logger.info("Found user details - Name: {}, Status: {}, Enabled: {}", 
                    user.getName(), user.getStatus(), user.isEnabled());
            }

            // If user is not found, return false
            if (userOptional.isEmpty()) {
                logger.warn("Password reset requested for non-existent email: {}", normalizedEmail);
                return false;
            }

            User user = userOptional.get();

            // Delete any existing reset tokens for this user
            passwordResetTokenRepository.deleteByUser(user);

            // Generate new token
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUser(user);
            resetToken.setExpiryDate(LocalDateTime.now().plusHours(1));
            resetToken.setUsed(false);
            passwordResetTokenRepository.save(resetToken);

            // Send reset email with token
            String targetEmail = user.getEmail() != null ? user.getEmail() : normalizedEmail;
            boolean emailSent = productionEmailService.sendCustomEmail(
                targetEmail,
                "Restablecer contraseña - Creparis",
                buildResetEmailBody(user.getName(), token)
            );

            if (emailSent) {
                logger.info("Password reset link sent to: {}", targetEmail);
                return true;
            } else {
                logger.error("Failed to send email via Resend");
                return false;
            }
        } catch (Exception e) {
            logger.error("Unexpected error in password reset process", e);
            return false;
        }
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> resetTokenOptional = 
            passwordResetTokenRepository.findByTokenAndUsedFalseAndExpiryDateAfter(
                token, LocalDateTime.now()
            );

        if (resetTokenOptional.isEmpty()) {
            logger.warn("Invalid or expired reset token: {}", token);
            return false;
        }

        PasswordResetToken resetToken = resetTokenOptional.get();
        User user = resetToken.getUser();

        // Encode the new password
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        logger.info("Password successfully reset for user: {}", user.getEmail());
        return true;
    }

    public boolean validateResetToken(String token) {
        Optional<PasswordResetToken> resetTokenOptional = 
            passwordResetTokenRepository.findByTokenAndUsedFalseAndExpiryDateAfter(
                token, LocalDateTime.now()
            );

        return resetTokenOptional.isPresent();
    }

    public void sendPasswordResetConfirmationEmail(String token) {
        try {
            // Find the user associated with this token
            Optional<PasswordResetToken> resetTokenOptional = passwordResetTokenRepository.findByToken(token);

            if (resetTokenOptional.isPresent()) {
                PasswordResetToken resetToken = resetTokenOptional.get();
                if (resetToken.getExpiryDate() == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                    logger.warn("Reset token expired; skipping confirmation email for token: {}", token);
                    return;
                }

                User user = resetToken.getUser();
                
                // Send confirmation email
                boolean emailSent = productionEmailService.sendCustomEmail(
                    user.getEmail(),
                    "Contraseña actualizada - Creparis",
                    buildPasswordResetConfirmationEmailBody(user.getName())
                );

                if (emailSent) {
                    logger.info("Password reset confirmation email sent to: {}", user.getEmail());
                } else {
                    logger.error("Failed to send confirmation email via Resend");
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error in sending password reset confirmation email", e);
        }
    }

    private String buildResetEmailBody(String userName, String token) {
        String base = frontendUrl == null ? "" : frontendUrl.trim();
        String resetUrl = base.endsWith("/") ? (base + "ForgotPassword/" + token) : (base + "/ForgotPassword/" + token);
        String safeName = (userName == null || userName.isBlank()) ? "" : userName.trim();
        return String.format(
            "<html>" +
            "<head>" +
            "  <meta charset='UTF-8'/>" +
            "  <meta name='viewport' content='width=device-width, initial-scale=1.0'/>" +
            "</head>" +
            "<body style='margin:0; padding:0; background:#f4f6fb; font-family: Arial, sans-serif;'>" +
            "  <div style='max-width: 640px; margin: 0 auto; padding: 28px 16px;'>" +
            "    <div style='background: linear-gradient(135deg, #0ea5e9 0%%, #22c55e 100%%); border-radius: 14px 14px 0 0; padding: 22px 20px; color: #ffffff;'>" +
            "      <div style='font-size: 14px; opacity: 0.95; letter-spacing: 0.6px;'>Creparis</div>" +
            "      <div style='font-size: 22px; font-weight: 700; margin-top: 6px;'>Restablecimiento de contraseña</div>" +
            "      <div style='margin-top: 8px; font-size: 14px; opacity: 0.95;'>Se solicitó un cambio de contraseña para tu cuenta.</div>" +
            "    </div>" +
            "    <div style='background:#ffffff; border-radius: 0 0 14px 14px; box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08); padding: 22px 20px;'>" +
            "      <p style='margin: 0; color:#0f172a; font-size: 14px; line-height: 1.7;'>" +
            "        Hola%s, te compartimos un enlace seguro para actualizar tu contraseña. Este enlace expira en <strong>1 hora</strong>." +
            "      </p>" +
            "      <div style='margin-top: 16px; padding: 14px 14px; border-radius: 12px; background: #f8fafc; border: 1px solid #e2e8f0;'>" +
            "        <div style='font-size: 13px; color:#334155; font-weight: 700; margin-bottom: 10px;'>Token</div>" +
            "        <div style='font-size: 13px; color:#0f172a; line-height: 1.8;'>" +
            "          <span style='display:inline-block; background:#0f172a; color:#ffffff; padding: 8px 10px; border-radius: 10px; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, \"Liberation Mono\", \"Courier New\", monospace; letter-spacing: 0.4px; word-break: break-all;'>%s</span>" +
            "        </div>" +
            "        <div style='margin-top: 10px; font-size: 12px; color:#64748b; line-height: 1.6;'>No compartas este token con nadie.</div>" +
            "      </div>" +
            "      <div style='text-align:center; margin-top: 18px;'>" +
            "        <a href='%s' style='display:inline-block; background:#0ea5e9; color:#ffffff; padding: 12px 18px; text-decoration:none; border-radius: 12px; font-weight: 700; font-size: 14px;'>Cambiar contraseña</a>" +
            "      </div>" +
            "      <p style='margin: 18px 0 0; color:#64748b; font-size: 12px; line-height: 1.6; text-align:center;'>" +
            "        Si no solicitaste este cambio, puedes ignorar este correo." +
            "      </p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>",
            safeName.isEmpty() ? "" : (", <strong>" + safeName + "</strong>"),
            token,
            resetUrl
        );
    }

    private String buildPasswordResetConfirmationEmailBody(String userName) {
        return String.format("""
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; }
                    .header { background: linear-gradient(135deg, #0ea5e9 0%, #22c55e 100%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .success-box { background-color: #ecfeff; border: 2px solid #22c55e; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center; }
                    .footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✅ Contraseña Actualizada</h1>
                        <p>Creparis</p>
                    </div>
                    <div class="content">
                        <h2>Hola %s,</h2>
                        
                        <div class="success-box">
                            <h3>¡Tu contraseña ha sido actualizada exitosamente!</h3>
                            <p>Tu cuenta en Creparis ahora tiene una nueva contraseña.</p>
                        </div>
                        
                        <p><strong>Información importante:</strong></p>
                        <ul>
                            <li>Tu contraseña ha sido cambiada recientemente</li>
                            <li>Si no realizaste este cambio, contacta inmediatamente a nuestro equipo de soporte</li>
                            <li>Para mayor seguridad, te recomendamos cambiar tu contraseña regularmente</li>
                        </ul>
                        
                        <p>Si tienes alguna pregunta o necesitas ayuda, no dudes en contactarnos.</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 Creparis. Todos los derechos reservados.</p>
                        <p>Este es un email automático, por favor no respondas a este mensaje.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName);
    }
}