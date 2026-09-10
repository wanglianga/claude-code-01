package com.eldercare.common;

import com.eldercare.security.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static AuthUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser u)) {
            throw new ApiException("未登录");
        }
        return u;
    }

    public static String role() {
        return get().role();
    }

    /** 系统线程（如启动种子）中没有登录态时返回 null */
    public static AuthUser getOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser u) {
            return u;
        }
        return null;
    }
}
