package com.opspilot.desk.dto.user;

import com.opspilot.desk.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private String role;
    private boolean active;
    private LocalDateTime createdAt;

    public static UserResponse from(User u) {
        UserResponse r = new UserResponse();
        r.setId(u.getId());
        r.setEmail(u.getEmail());
        r.setFullName(u.getFullName());
        r.setRole(u.getRole().name());
        r.setActive(u.isActive());
        r.setCreatedAt(u.getCreatedAt());
        return r;
    }
}
