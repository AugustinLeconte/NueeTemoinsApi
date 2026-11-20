package com.nueetemoins.backend.profile;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final JdbcTemplate jdbc;

    @Transactional
    public void delete(String userId) {
        jdbc.update("DELETE FROM friendships WHERE requester_id = ? OR addressee_id = ?", userId, userId);
        jdbc.update("DELETE FROM profiles WHERE user_id = ?", userId);
    }
}