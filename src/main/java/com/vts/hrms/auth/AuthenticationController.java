package com.vts.hrms.auth;

import com.vts.hrms.cfg.JwtUtil;
import com.vts.hrms.entity.Login;
import com.vts.hrms.entity.Role;
import com.vts.hrms.repository.LoginRepository;
import com.vts.hrms.repository.RoleRepository;
import com.vts.hrms.service.MasterClientService;
import com.vts.hrms.util.ErrorResponse;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);
    private static final Duration ALLOWED_TIMESTAMP_SKEW = Duration.ofMinutes(5);
    private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a", Locale.ENGLISH).withZone(ZoneId.systemDefault());


    @Value("${is_mdm_connect}")
    private boolean isMdmConnect;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MasterClientService masterClientService;

    @Autowired
    private LoginRepository loginRepository;


    @CrossOrigin("*")
    @PostMapping("/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) {

        if (authenticationRequest == null) {
            log.warn("Authentication request is empty.");
            return error(HttpStatus.BAD_REQUEST, "EMPTY_CREDENTIALS", "Please enter your username and password.");
        }

        String username = authenticationRequest.getUsername();
        String password = authenticationRequest.getPassword();

        if (username == null || username.trim().isEmpty()) {
            log.warn("Authentication failed: username is missing.");
            return error(HttpStatus.BAD_REQUEST, "EMPTY_USERNAME", "Please enter your username.");
        }

        if (password == null || password.trim().isEmpty()) {
            log.warn("Authentication failed: password is missing for username '{}'", username);
            return error(HttpStatus.BAD_REQUEST, "EMPTY_PASSWORD", "Please enter your password.");
        }

        username = username.trim();

        ResponseEntity<ErrorResponse> timestampError = validateTimestamp(authenticationRequest, username);
        if (timestampError != null) {
            return timestampError;
        }

        if (isMdmConnect) {
            return authenticateThroughMdm(authenticationRequest, username);
        }

        return authenticateLocally(username, password);
    }

    private ResponseEntity<ErrorResponse> validateTimestamp(
            AuthenticationRequest request,
            String username) {

        String timestamp = request.getTimestamp();
        if (timestamp == null || timestamp.trim().isEmpty()) {
            log.warn("Authentication rejected for username '{}': request timestamp is missing.", username);
            return error(
                    HttpStatus.BAD_REQUEST,
                    "TIMESTAMP_REQUIRED",
                    "Your device time could not be verified. Refresh the page and try again."
            );
        }

        final Instant clientTime;
        try {
            clientTime = Instant.parse(timestamp);
        } catch (DateTimeParseException e) {
            log.warn("Authentication rejected for username '{}': request timestamp is not a valid ISO-8601 instant.", username);
            return error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TIMESTAMP",
                    "The request timestamp is invalid. Refresh the page and try again."
            );
        }

        Instant serverTime = Instant.now();
        Duration clockSkew = Duration.between(clientTime, serverTime);
        if (clockSkew.abs().compareTo(ALLOWED_TIMESTAMP_SKEW) > 0) {
            String formattedClientTime = DISPLAY_DATE_TIME_FORMAT.format(clientTime);
            String formattedServerTime = DISPLAY_DATE_TIME_FORMAT.format(serverTime);
            log.warn(
                    "Authentication rejected for username '{}': client time={}, server time={}, clock skew={} seconds; allowed skew is {} seconds.",
                    username,
                    clientTime,
                    serverTime,
                    clockSkew.abs().getSeconds(),
                    ALLOWED_TIMESTAMP_SKEW.getSeconds()
            );
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "CLOCK_SKEW",
                    String.format("Device time: %s; server time: %s. Please sync your device clock.", formattedClientTime, formattedServerTime)
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }

        return null;
    }

    private ResponseEntity<?> authenticateThroughMdm(AuthenticationRequest request, String username) {
        try {
            ErrorResponse body = null;
            ResponseEntity<ErrorResponse> response = masterClientService.getAuthenticate(request);
            body = response != null ? response.getBody() : null;

            if (body != null && !(body.getStatus() == 200)) {
                log.warn(
                        "MDM rejected login for username  '{}'. Status={}, code={}, message={}",
                        username,
                        body.getStatus(),
                        body.getErrorCode(),
                        body.getMessage()
                );
                return error(HttpStatus.valueOf(body.getStatus()), body.getErrorCode(), body.getMessage());
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            String token = jwtUtil.generateToken(userDetails);

            Set<String> roles = getRoles(userDetails);
            Set<String> hindiRoles = getHindiRoles(roles);

            assert body != null;
            return ResponseEntity.ok(new AuthenticationResponse(token, roles, hindiRoles, body.getErrorCode()));

        } catch (FeignException e) {
            log.warn(
                    "MDM authentication failed for username '{}'. Status={}, response={}",
                    username,
                    e.status(),
                    e.contentUTF8()
            );

            return error(
                    HttpStatus.EXPECTATION_FAILED,
                    "MDM",
                    "We couldn't find an account matching your login details."
            );

        } catch (UsernameNotFoundException e) {
            log.warn("Local account not found for MDM-authenticated username '{}'.", username);

            return error(
                    HttpStatus.UNAUTHORIZED,
                    "USER_NOT_FOUND",
                    "We couldn't find an account matching your login details."
            );

        } catch (Exception e) {
            log.error("Unexpected error during MDM authentication for username '{}'.", username, e);

            return error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INTERNAL_ERROR",
                    "Something went wrong while signing you in. Please try again later."
            );
        }
    }

    private ResponseEntity<?> authenticateLocally(String username, String password) {
        try {
            UserDetails userDetails;

            try {
                userDetails = userDetailsService.loadUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                log.warn("Login attempt failed: username '{}' not found.", username);
                return error(
                        HttpStatus.UNAUTHORIZED,
                        "USER_NOT_FOUND",
                        "We couldn't find an account with that username. Please check your username and try again."
                );
            }

            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

            String token = jwtUtil.generateToken(userDetails);

            Set<String> roles = getRoles(userDetails);
            Set<String> hindiRoles = getHindiRoles(roles);

            Login login = loginRepository.findByUsernameAndIsActive(username, 1);
            if (login == null) {
                log.warn("Login attempt failed: username '{}' not found.", username);
                return error(
                        HttpStatus.NO_CONTENT,
                        "DATA_NOT_FOUND",
                        "Login details not found with this username."
                );
            }

            boolean isDefaultPassword = login.getIsDefaultPassword().equalsIgnoreCase("Y");

            return ResponseEntity.ok(
                    new AuthenticationResponse(
                            token,
                            roles,
                            hindiRoles,
                            isDefaultPassword ? "DEFAULT_PASSWORD" : "AUTHENTICATED"
                    )
            );

        } catch (BadCredentialsException e) {
            log.warn("Authentication failed for username '{}': invalid password.", username);
            return error(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "The password you entered is incorrect. Please try again."
            );

        } catch (DisabledException e) {
            log.warn("Authentication failed for username '{}': account is disabled.", username);
            return error(
                    HttpStatus.FORBIDDEN,
                    "USER_DISABLED",
                    "Your account is currently disabled. Please contact your administrator."
            );

        } catch (LockedException e) {
            log.warn("Authentication failed for username '{}': account is locked.", username);
            return error(
                    HttpStatus.FORBIDDEN,
                    "USER_LOCKED",
                    "Your account is locked. Please contact your administrator."
            );

        } catch (UsernameNotFoundException e) {
            log.warn("Authentication failed for username '{}': account not found during local validation.", username);
            return error(
                    HttpStatus.UNAUTHORIZED,
                    "USER_NOT_FOUND",
                    "We couldn't find an account with that username. Please check your username and try again."
            );

        } catch (AuthenticationException e) {
            log.warn("Authentication failed for username '{}': authentication exception.", username, e);
            return error(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_FAILED",
                    "We couldn't verify your login details. Please try again."
            );

        } catch (Exception e) {
            log.error("Unexpected error during local authentication for username '{}'.", username, e);
            return error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INTERNAL_ERROR",
                    "Something went wrong while signing you in. Please try again later."
            );
        }
    }

    private Set<String> getRoles(UserDetails userDetails) {
        return userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private Set<String> getHindiRoles(Set<String> roles) {
        List<Role> roleList = roleRepository.findAll();

        return roleList.stream()
                .filter(role -> roles.contains(role.getRoleName()))
                .map(Role::getHindiRoleName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String errorCode,
            String message) {

        return ResponseEntity.status(status)
                .body(new ErrorResponse(
                        status.value(),
                        errorCode,
                        message
                ));
    }


}
