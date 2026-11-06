package com.vts.hrms.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {

    private String token;
    private Set<String> roles;
    private Set<String> hindiRoles;
    private String statusCode;

}
