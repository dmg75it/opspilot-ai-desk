package com.opspilot.desk.service;

import com.opspilot.desk.dto.UserDto;
import com.opspilot.desk.entity.AppUser;
import com.opspilot.desk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public List<UserDto> listAll() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    public Optional<AppUser> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<AppUser> findById(UUID id) {
        return userRepository.findById(id);
    }

    private UserDto toDto(AppUser u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name(), u.isEnabled(), u.getCreatedAt());
    }
}
