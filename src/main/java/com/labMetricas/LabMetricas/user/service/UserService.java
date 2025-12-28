package com.labMetricas.LabMetricas.user.service;

import com.labMetricas.LabMetricas.enums.TypeResponse;
import com.labMetricas.LabMetricas.role.repository.RoleRepository;
import com.labMetricas.LabMetricas.user.model.User;
import com.labMetricas.LabMetricas.user.repository.UserRepository;
import com.labMetricas.LabMetricas.user.model.dto.ChangePasswordDto;
import com.labMetricas.LabMetricas.user.model.dto.UserDto;
import com.labMetricas.LabMetricas.util.ResponseObject;
import com.labMetricas.LabMetricas.config.ProductionEmailService;
import com.labMetricas.LabMetricas.auditLog.model.AuditLog;
import com.labMetricas.LabMetricas.auditLog.repository.AuditLogRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final int TEMP_PASSWORD_LENGTH = 12;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProductionEmailService productionEmailService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    // Método helper para crear logs de auditoría
    private void createAuditLog(String action, User user) {
        try {
            // Obtener el usuario actual autenticado
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = null;
            
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                Optional<User> userOpt = userRepository.findByEmail(email);
                currentUser = userOpt.orElse(null);
            }
            
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setUser(currentUser);
            auditLog.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(auditLog);
            
            logger.debug("Audit log created: {} by user: {}", action, 
                currentUser != null ? currentUser.getEmail() : "ANONYMOUS");
        } catch (Exception e) {
            logger.error("Error creating audit log: {}", action, e);
            // No lanzar excepción para no interrumpir el flujo principal
        }
    }

    // Method to send welcome email with temporary password
    private void sendWelcomeEmail(String email, String temporaryPassword, String name) {
        try {
            String subject = "¡Bienvenido a Creparis! Tus credenciales de acceso";
            String htmlContent = buildTemporaryPasswordEmailBody(name, email, temporaryPassword);

            boolean onboardingEmailSent = productionEmailService.sendCustomEmail(email, subject, htmlContent);
            if (onboardingEmailSent) {
                logger.info("Welcome email with credentials sent to: {}", email);
            } else {
                logger.error("Failed to send welcome email with credentials to: {}", email);
            }
        } catch (Exception e) {
            logger.error("Failed to send welcome email to: {}", email, e);
        }
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            int idx = secureRandom.nextInt(TEMP_PASSWORD_CHARS.length());
            sb.append(TEMP_PASSWORD_CHARS.charAt(idx));
        }
        return sb.toString();
    }

    // Helper method to build a stylish welcome email body
    private String buildTemporaryPasswordEmailBody(String name, String email, String temporaryPassword) {
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
            "      <div style='font-size: 22px; font-weight: 700; margin-top: 6px;'>¡Bienvenido, %s!</div>" +
            "      <div style='margin-top: 8px; font-size: 14px; opacity: 0.95;'>Tu cuenta ha sido creada correctamente.</div>" +
            "    </div>" +
            "    <div style='background:#ffffff; border-radius: 0 0 14px 14px; box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08); padding: 22px 20px;'>" +
            "      <p style='margin: 0; color:#0f172a; font-size: 14px; line-height: 1.7;'>" +
            "        Nos complace darte la bienvenida a <strong>Creparis</strong>, tu plataforma para la gestión de productos, inventario y trazabilidad." +
            "      </p>" +
            "      <div style='margin-top: 16px; padding: 14px 14px; border-radius: 12px; background: #f8fafc; border: 1px solid #e2e8f0;'>" +
            "        <div style='font-size: 13px; color:#334155; font-weight: 700; margin-bottom: 10px;'>Tus credenciales</div>" +
            "        <div style='font-size: 13px; color:#0f172a; line-height: 1.8;'>" +
            "          <div><span style='color:#64748b;'>Correo:</span> <strong>%s</strong></div>" +
            "          <div><span style='color:#64748b;'>Contraseña temporal:</span> <span style='display:inline-block; background:#0f172a; color:#ffffff; padding: 6px 10px; border-radius: 10px; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, " +
            "\"Liberation Mono\", \"Courier New\", monospace; letter-spacing: 0.6px;'>%s</span></div>" +
            "        </div>" +
            "        <div style='margin-top: 10px; font-size: 12px; color:#64748b; line-height: 1.6;'>Por seguridad, cambia tu contraseña al iniciar sesión por primera vez.</div>" +
            "      </div>" +
            "      <div style='margin-top: 16px; padding: 14px 14px; border-radius: 12px; background: #ecfeff; border: 1px solid #a5f3fc;'>" +
            "        <div style='font-size: 13px; color:#0f172a; font-weight: 700; margin-bottom: 8px;'>Con tu cuenta podrás:</div>" +
            "        <ul style='margin: 0; padding-left: 18px; color:#0f172a; font-size: 13px; line-height: 1.8;'>" +
            "          <li>Gestionar catálogos y productos</li>" +
            "          <li>Administrar inventario y movimientos</li>" +
            "          <li>Consultar códigos QR con información detallada</li>" +
            "          <li>Acceder a reportes y trazabilidad</li>" +
            "        </ul>" +
            "      </div>" +
            "      <div style='text-align:center; margin-top: 18px;'>" +
            "        <a href='%s' style='display:inline-block; background:#0ea5e9; color:#ffffff; padding: 12px 18px; text-decoration:none; border-radius: 12px; font-weight: 700; font-size: 14px;'>Iniciar sesión</a>" +
            "      </div>" +
            "      <p style='margin: 18px 0 0; color:#64748b; font-size: 12px; line-height: 1.6; text-align:center;'>" +
            "        Si no solicitaste esta cuenta, por favor contacta con soporte." +
            "      </p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>",
            name, email, temporaryPassword, frontendUrl
        );
    }

    @Transactional
    public ResponseEntity<ResponseObject> createUser(UserDto userDto) {
        try {
            String normalizedEmail = userDto.getEmail() == null ? null : userDto.getEmail().trim().toLowerCase();
            if (normalizedEmail == null || normalizedEmail.isBlank()) {
                return ResponseEntity.badRequest().body(
                    new ResponseObject("Email is required", null, TypeResponse.ERROR)
                );
            }

            // Check if email already exists
            if (userRepository.existsByEmailNormalized(normalizedEmail)) {
                return ResponseEntity.badRequest().body(
                    new ResponseObject("Email already exists", null, TypeResponse.ERROR)
                );
            }

            User user = new User();
            user.setName(userDto.getName());
            user.setEmail(normalizedEmail);
            user.setPosition(userDto.getPosition());
            user.setPhone(userDto.getPhone());
            user.setStatus(userDto.getStatus() != null ? userDto.getStatus() : true);
            user.setRole(roleRepository.findById(userDto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found")));
            String rawPassword = userDto.getPassword() != null && !userDto.getPassword().isBlank()
                ? userDto.getPassword()
                : generateTemporaryPassword();
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            User savedUser = userRepository.save(user);
            UserDto responseDto = convertToDto(savedUser);
            responseDto.setTemporaryPassword(rawPassword);
            sendWelcomeEmail(savedUser.getEmail(), rawPassword, savedUser.getName());
            
            // Registrar log de auditoría
            createAuditLog(String.format("Se agregó un usuario: %s (%s)", savedUser.getName(), savedUser.getEmail()), savedUser);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(
                new ResponseObject("User created successfully", responseDto, TypeResponse.SUCCESS)
            );
        } catch (Exception e) {
            logger.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ResponseObject("Error creating user", null, TypeResponse.ERROR)
            );
        }
    }

    @Transactional
    public ResponseEntity<ResponseObject> updateUser(UserDto userDto) {
        try {
            // Find existing user
            User existingUser = userRepository.findById(userDto.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Store old email for notification
            String oldEmail = existingUser.getEmail();

            // Update user details
            existingUser.setName(userDto.getName());
            existingUser.setPosition(userDto.getPosition());
            existingUser.setPhone(userDto.getPhone());

            String normalizedEmail = userDto.getEmail() == null ? null : userDto.getEmail().trim().toLowerCase();
            if (normalizedEmail == null || normalizedEmail.isBlank()) {
                return ResponseEntity.badRequest().body(
                    new ResponseObject("Email is required", null, TypeResponse.ERROR)
                );
            }

            Optional<User> user = userRepository.findByEmailNormalized(normalizedEmail);
            if (user.isPresent() && !user.get().getId().equals(existingUser.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        new ResponseObject("Email already exists", null, TypeResponse.ERROR));
            } else {
                existingUser.setEmail(normalizedEmail);
            }

            // Update role if changed
            if (!existingUser.getRole().getId().equals(userDto.getRoleId())) {
                existingUser.setRole(roleRepository.findById(userDto.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found")));
            }

            // Update timestamps
            existingUser.setUpdatedAt(LocalDateTime.now());

            // Save updated user
            User updatedUser = userRepository.save(existingUser);

            // Send notification about user update
            sendUserUpdateNotification(updatedUser, oldEmail);

            // Convert to DTO for response
            UserDto responseDto = convertToDto(updatedUser);

            // Registrar log de auditoría
            createAuditLog(String.format("Se actualizó el usuario: %s (%s)", updatedUser.getName(), updatedUser.getEmail()), updatedUser);

            return ResponseEntity.ok(
                new ResponseObject("User updated successfully", responseDto, TypeResponse.SUCCESS)
            );
        } catch (Exception e) {
            logger.error("Error updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ResponseObject("Error updating user", null, TypeResponse.ERROR)
            );
        }
    }

    public ResponseEntity<ResponseObject> getUserById(UUID id) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(
                new ResponseObject("User retrieved successfully", convertToDto(user), TypeResponse.SUCCESS)
            );
        } catch (Exception e) {
            logger.error("Error retrieving user", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ResponseObject("User not found", null, TypeResponse.ERROR)
            );
        }
    }

    public ResponseEntity<ResponseObject> getAllUsers() {
        try {
            List<UserDto> users = userRepository.findAllWithRoles().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

            // NO registrar log de auditoría para consultas (evitar spam)

            return ResponseEntity.ok(
                new ResponseObject("Users retrieved successfully", users, TypeResponse.SUCCESS)
            );
        } catch (Exception e) {
            logger.error("Error retrieving users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ResponseObject("Error retrieving users", null, TypeResponse.ERROR)
            );
        }
    }

    public ResponseEntity<ResponseObject> getAllUsersButMe(String userEmail) {
        try {
            String normalized = userEmail.trim().toLowerCase();

            List<UserDto> users = userRepository.findAllWithRoles().stream()
                    .filter(u -> u.getEmail() == null || !normalized.equals(u.getEmail().trim().toLowerCase()))
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            // NO registrar log de auditoría para consultas (evitar spam)

            return ResponseEntity.ok(
                    new ResponseObject("Users retrieved successfully (excluding current)", users, TypeResponse.SUCCESS)
            );
        } catch (Exception e) {
            logger.error("Error retrieving users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ResponseObject("Error retrieving users", null, TypeResponse.ERROR)
            );
        }
    }

    @Transactional
    public ResponseEntity<ResponseObject> deleteUser(UUID id) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Soft delete
            user.setStatus(false);
            user.setDeletedAt(LocalDateTime.now());
            userRepository.save(user);

            // Registrar log de auditoría
            createAuditLog(String.format("Se eliminó el usuario: %s (%s)", user.getName(), user.getEmail()), user);

            return ResponseEntity.ok(
                new ResponseObject("User deleted successfully", null, TypeResponse.SUCCESS)
            );
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ResponseObject("Error deleting user", null, TypeResponse.ERROR)
            );
        }
    }

    @Transactional
    public ResponseEntity<ResponseObject> changePassword(ChangePasswordDto changePasswordDto) {
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify current password
            if (!passwordEncoder.matches(changePasswordDto.getCurrentPassword(), currentUser.getPassword())) {
                return ResponseEntity.badRequest().body(
                    new ResponseObject("Current password is incorrect", null, TypeResponse.ERROR)
                );
            }

            // Update password
            currentUser.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
            currentUser.setUpdatedAt(LocalDateTime.now());
            userRepository.save(currentUser);

            // Send password change confirmation email
            sendPasswordChangeConfirmation(currentUser);

            // Registrar log de auditoría
            createAuditLog(String.format("Se cambió la contraseña del usuario: %s (%s)", currentUser.getName(), currentUser.getEmail()), currentUser);

            return ResponseEntity.ok(
                new ResponseObject("Password changed successfully", null, TypeResponse.SUCCESS)
            );
        } catch (Exception e) {
            logger.error("Error changing password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ResponseObject("Error changing password", null, TypeResponse.ERROR)
            );
        }
    }

    @Transactional
    public ResponseEntity<ResponseObject> toggleUserStatus(UUID userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            boolean oldStatus = user.getStatus();
            user.setStatus(!user.getStatus());
            user.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userRepository.save(user);

            // Send status change notification
            sendUserStatusChangeNotification(updatedUser, oldStatus);

            // Registrar log de auditoría
            String statusText = updatedUser.getStatus() ? "Activo" : "Inactivo";
            createAuditLog(String.format("Se cambió el estado del usuario: %s (%s) a %s", 
                updatedUser.getName(), updatedUser.getEmail(), statusText), updatedUser);

            return ResponseEntity.ok(
                new ResponseObject("User status updated successfully", convertToDto(updatedUser), TypeResponse.SUCCESS)
            );
        } catch (Exception e) {
            logger.error("Error toggling user status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ResponseObject("Error updating user status", null, TypeResponse.ERROR)
            );
        }
    }

    // Email notification methods
    private void sendUserUpdateNotification(User user, String oldEmail) {
        return;
    }

    private void sendPasswordChangeConfirmation(User user) {
        return;
    }

    private void sendUserStatusChangeNotification(User user, boolean oldStatus) {
        return;
    }

    // Method to get user by email
    public ResponseEntity<ResponseObject> getUserByEmail(String email) {
        try {
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

            // NO registrar log de auditoría para consultas (evitar spam)

            return ResponseEntity.ok(
                new ResponseObject("User retrieved successfully", convertToDto(user), TypeResponse.SUCCESS)
            );
        } catch (Exception e) {
            logger.error("Error retrieving user by email", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ResponseObject("User not found", null, TypeResponse.ERROR)
            );
        }
    }

    // Method to delete user by email
    @Transactional
    public ResponseEntity<ResponseObject> deleteUserByEmail(String email) {
        try {
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Soft delete
            user.setStatus(false);
            user.setDeletedAt(LocalDateTime.now());
            userRepository.saveAndFlush(user);

            // Registrar log de auditoría
            createAuditLog(String.format("Se eliminó el usuario con email: %s", email), user);

            return ResponseEntity.ok(
                new ResponseObject("User deleted successfully", null, TypeResponse.SUCCESS)
            );
        } catch (Exception e) {
            logger.error("Error deleting user by email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ResponseObject("Error deleting user", null, TypeResponse.ERROR)
            );
        }
    }

    // Method to find user by email
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    // Helper method to convert User to UserDto
    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPosition(user.getPosition());
        dto.setPhone(user.getPhone());
        dto.setStatus(user.getStatus());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setRoleId(user.getRole() != null ? user.getRole().getId() : null);
        return dto;









    }
} 