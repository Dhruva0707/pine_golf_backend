package com.pinewoods.score.tracker;

import org.springframework.security.core.context.SecurityContextHolder;

public class Utilities {
    public static boolean isUserAdmin() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}
