package com.opspilot.desk.controller;

import com.opspilot.desk.dto.DashboardResponse;
import com.opspilot.desk.entity.AppUser;
import com.opspilot.desk.repository.UserRepository;
import com.opspilot.desk.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal UserDetails principal) {
        AppUser user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        return ResponseEntity.ok(dashboardService.getDashboard(user));
    }
}
