package com.fontal.cookagent.security;

/** 线程本地存储的当前登录用户上下文。 */
public class CurrentUser {

    private static final ThreadLocal<AuthPrincipal> HOLDER = new ThreadLocal<>();

    public static void set(AuthPrincipal principal) {
        HOLDER.set(principal);
    }

    public static AuthPrincipal get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static Long requireUserId() {
        AuthPrincipal p = get();
        if (p == null) {
            throw new IllegalStateException("当前请求未登录");
        }
        return p.userId();
    }

    public record AuthPrincipal(Long userId, String username, String role) {}
}