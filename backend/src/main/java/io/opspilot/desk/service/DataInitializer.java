package io.opspilot.desk.service;

import io.opspilot.desk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        updatePasswordIfPlaceholder("admin@example.com", "admin123");
        updatePasswordIfPlaceholder("operator@example.com", "operator123");
    }

    private void updatePasswordIfPlaceholder(String email, String rawPassword) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if ("$PLACEHOLDER$".equals(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
                log.info("Password initialized for {}", email);
            }
        });
    }
}
