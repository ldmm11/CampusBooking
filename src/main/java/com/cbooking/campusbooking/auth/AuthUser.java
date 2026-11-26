package com.cbooking.campusbooking.auth;

import java.time.OffsetDateTime;

public record AuthUser(String id, String name, String nickname, String role,
                       String status, OffsetDateTime bannedUntil, OffsetDateTime createdAt) {
    public boolean enabled() { return "ENABLED".equals(status); }
}
