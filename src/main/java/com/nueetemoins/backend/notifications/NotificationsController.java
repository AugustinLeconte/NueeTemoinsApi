package com.nueetemoins.backend.notifications;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationsController {
    private final JdbcTemplate jdbc;

    public record Summary(int friendRequests) {}

    @GetMapping("/summary")
    public Summary summary(@RequestAttribute("userId") String userId) {
        Integer n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM friendships WHERE addressee_id = ? AND status = 'pending'",
            Integer.class, userId);
        return new Summary(n == null ? 0 : n);
    }
}