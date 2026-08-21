package com.vts.hrms.auth;

import com.vts.hrms.cfg.JwtUtil;
import com.vts.hrms.entity.Role;
import com.vts.hrms.repository.LoginRepository;
import com.vts.hrms.repository.RoleRepository;
import com.vts.hrms.service.MasterClientService;
import com.vts.hrms.util.ApiResponse;
import feign.FeignException;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class AuthenticationController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private LoginRepository loginRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MasterClientService masterClientService;

    @Value("${labCode}")
    private String labCode;

    private static final long TIMESTAMP_TOLERANCE = 300000;

    @CrossOrigin("*")
    @RequestMapping(value = "/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest)
            throws Exception {
        if(labCode.equalsIgnoreCase("CAIR")) {
            LOG.info(" Inside createAuthenticationToken to authenticate from MDM ");
            try {
                ResponseEntity<String> response = masterClientService.getAuthenticate(authenticationRequest);

                // Only runs when MDM returns 200
                if (response.getStatusCode().is2xxSuccessful()) {
                    UserDetails userdetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
                    String token = jwtUtil.generateToken(userdetails);

                   List<Role> roleList = roleRepository.findAll();


                    Set<String> roles = userdetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());

                    Set<String> hindiRoles = roleList.stream()
                            .filter(role -> roles.contains(role.getRoleName()))
                            .map(Role::getHindiRoleName)
                            .collect(Collectors.toSet());

                    return ResponseEntity.ok(new ApiResponse(true, "Login successful", new AuthenticationResponse(token,roles,hindiRoles)));
                }

            }
            catch (FeignException.Unauthorized e) {
                LOG.error(" Invalid username or password while authenticate from MDM: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
            }
            catch (FeignException.Forbidden e) {
                LOG.error(" User account is disabled while authenticate from MDM: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User account is disabled");

            }

            LOG.error(" Authentication failed while authenticate from MDM ");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
        } else {
            try {

                LOG.info("Authentication attempt for user: {}", authenticationRequest.getUsername());

                // 1. Timestamp validation (anti-replay)
                long currentTime = System.currentTimeMillis();
//                long requestTime = authenticationRequest.getTimestamp();

//                if (Math.abs(currentTime - requestTime) > TIMESTAMP_TOLERANCE) {
//                    LOG.warn("Request expired for user: {}", authenticationRequest.getUsername());
//                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                            .body(new ApiResponse(false, "Request expired or invalid timestamp", null));
//                }

                // 2. Decrypt password (Base64 decode)
                String decryptedPassword = new String(Base64.getDecoder()
                        .decode(authenticationRequest.getPassword()), StandardCharsets.UTF_8);

                // 3. Basic input validation
                String username = authenticationRequest.getUsername();
                if (username == null || username.trim().isEmpty() || decryptedPassword.isEmpty()) {
                    LOG.warn("Invalid credentials format for user: {}", username);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ApiResponse(false, "Invalid input format", null));
                }

                // 4. Authenticate
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(username.trim(), decryptedPassword)
                );

                // 5. Generate JWT
                UserDetails userDetails = userDetailsService.loadUserByUsername(username.trim());
                String token = jwtUtil.generateToken(userDetails);

                Set<String> roles = userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

                List<Role> roleList = roleRepository.findAll();


                Set<String> hindiRoles = roleList.stream()
                        .filter(role -> roles.contains(role.getRoleName()))
                        .map(Role::getHindiRoleName)
                        .collect(Collectors.toSet());


                LOG.info("Authentication successful for user: {}", username);
                return ResponseEntity.ok(new ApiResponse(true, "Login successful", new AuthenticationResponse(token,roles,hindiRoles)));

            } catch (DisabledException e) {
                LOG.warn("Disabled account login attempt: {}", authenticationRequest.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Your account is disabled", null));
            } catch (LockedException e) {
                LOG.warn("Locked account login attempt: {}", authenticationRequest.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Your account is locked", null));
            } catch (BadCredentialsException e) {
                LOG.warn("Invalid credentials for user: {}", authenticationRequest.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "Invalid username or password", null));
            } catch (Exception e) {
                LOG.error("Authentication error for user: {}", authenticationRequest.getUsername(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse(false, "Authentication failed", null));
            }
        }
    }


//    @RequestMapping(value = "/refreshtoken", method = RequestMethod.GET)
//    public ResponseEntity<?> refreshtoken(HttpServletRequest request) throws Exception {
//        // From the HttpRequest get the claims
//        DefaultClaims claims = (DefaultClaims) request.getAttribute("claims");
//
//        Map<String, Object> expectedMap = getMapFromIoJsonwebtokenClaims(claims);
//        String token = jwtUtil.doGenerateRefreshToken(expectedMap, expectedMap.get("sub").toString());
//        return ResponseEntity.ok(new AuthenticationResponse(token));
//    }


//    public Map<String, Object> getMapFromIoJsonwebtokenClaims(DefaultClaims claims) {
//        Map<String, Object> expectedMap = new HashMap<String, Object>();
//        for (Map.Entry<String, Object> entry : claims.entrySet()) {
//            expectedMap.put(entry.getKey(), entry.getValue());
//        }
//        return expectedMap;
//    }


}
