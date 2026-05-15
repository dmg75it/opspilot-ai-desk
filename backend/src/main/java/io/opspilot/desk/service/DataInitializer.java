package io.opspilot.desk.service;

import io.opspilot.desk.entity.Role;
import io.opspilot.desk.entity.User;
import io.opspilot.desk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureUser("00000000-0000-0000-0000-000000000001",
                "admin@example.com", "admin123", "Admin User", Role.ADMIN);
        ensureUser("00000000-0000-0000-0000-000000000002",
                "operator@example.com", "operator123", "Operator User", Role.OPERATOR);
    }

    private void ensureUser(String id, String email, String rawPassword, String fullName, Role role) {
        userRepository.findByEmail(email).ifPresentOrElse(
                user -> {
                    // Update hash if it's still a placeholder
                    if (user.getPasswordHash().contains("PLACEHOLDER")) {
                        user.setPasswordHash(passwordEncoder.encode(rawPassword));
                        userRepository.save(user);
                        log.info("Seed user password initialized: email={}", email);
                    }
                },
                () -> {
                    var user = User.builder()
                            .id(UUID.fromString(id))
                            .email(email)
                            .passwordHash(passwordEncoder.encode(rawPassword))
                            .fullName(fullName)
                            .role(role)
                            .active(true)
                            .createdAt(Instant.now())
                            .build();
                    userRepository.save(user);
                    log.info("Seed user created: email={} role={}", email, role);
                }
        );
    }
}
