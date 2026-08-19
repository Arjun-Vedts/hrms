package com.vts.hrms.auth;

import java.util.Set;

public class AuthenticationResponse {

    private final String token;
    private final Set<String> roles; // Add this field
    private final Set<String> hindiRoles;

    public AuthenticationResponse(String token,Set<String> roles,Set<String> hindiRoles) {
        this.token = token;
        this.roles = roles;
        this.hindiRoles = hindiRoles;
    }

    public String getToken() { return token; }
    public Set<String> getRoles() { return roles; }
    public Set<String> getHindiRoles() { return hindiRoles;}


}
