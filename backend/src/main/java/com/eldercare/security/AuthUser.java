package com.eldercare.security;

import java.util.List;

public record AuthUser(Long id, String username, String realName, String role) {

    public List<String> roles() {
        return List.of(role);
    }
}
