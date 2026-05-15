package io.opspilot.desk.controller;

import io.opspilot.desk.dto.dashboard.DashboardStatsResponse;
import io.opspilot.desk.entity.User;
import io.opspilot.desk.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    DashboardStatsResponse stats(@AuthenticationPrincipal User user) {
        return dashboardService.getStats(user);
    }
}
