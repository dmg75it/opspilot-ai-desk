package com.opspilot.desk.config;

import com.opspilot.desk.entity.AppUser;
import com.opspilot.desk.entity.UserRole;
import com.opspilot.desk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        createUserIfNotExists("admin@example.com", "admin123", "Admin User", UserRole.ADMIN);
        createUserIfNotExists("operator@example.com", "operator123", "Field Operator", UserRole.OPERATOR);
    }

    private void createUserIfNotExists(String email, String password, String fullName, UserRole role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            AppUser user = AppUser.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .role(role)
                .enabled(true)
                .build();
            userRepository.save(user);
            log.info("Created seed user: {}", email);
        }
    }
}
