package jxmvc.core;

import jakarta.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface JxAuthProvider {
    boolean check(HttpServletRequest request, String[] requiredRoles);
}
