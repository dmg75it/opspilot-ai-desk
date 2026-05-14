package com.opspilot.desk.controller;

import com.opspilot.desk.dto.UserDto;
import com.opspilot.desk.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> listUsers() {
        return ResponseEntity.ok(userService.listAll());
    }
}
