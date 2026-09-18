package com.vts.hrms.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.vts.hrms.auth.AuthenticationController;
import com.vts.hrms.dto.*;
import com.vts.hrms.entity.AuditStamping;
import com.vts.hrms.entity.CashLimit;
import com.vts.hrms.entity.Login;
import com.vts.hrms.entity.RoleSecurity;
import com.vts.hrms.exception.BadRequestException;
import com.vts.hrms.repository.LoginRepository;
import com.vts.hrms.service.AdminService;
import com.vts.hrms.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/admin")
public class AdminController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;

    private final LoginRepository loginRepository;
    private DateTimeFormatter formatter;

    public AdminController(AdminService adminService,LoginRepository loginRepository) {

        this.adminService = adminService;
        this.loginRepository=loginRepository;
    }


    @GetMapping(value = "/roles")
    public ResponseEntity<List<RoleDTO>> getRoleList() {
        List<RoleDTO> list = adminService.getRoleList();

        return ResponseEntity.ok(list);
    }

    @GetMapping(value = "/user-list")
    public ResponseEntity<ApiResponse> getAllUsersList() {
        List<UserResponseDTO> list = adminService.getUserList();

        return ResponseEntity.ok(
                new ApiResponse(true, "User list fetched", list)
        );
    }

    @GetMapping(value = "/user/{loginId}")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable Long loginId) {
        UserResponseDTO dto = adminService.getUserById(loginId);

        return ResponseEntity.ok(
                new ApiResponse(true, "user data fetched", dto)
        );
    }

    @GetMapping("/exists/{username}")
    public boolean existsUsername(@PathVariable String username) {
        return adminService.checkUsernameExists(username);
    }

    @PostMapping(value = "/add-user")
    public ResponseEntity<ApiResponse> addNewUser(@RequestBody UserResponseDTO dto, @RequestHeader String username) {
        UserResponseDTO saved = adminService.addNewUser(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "User added successfully", saved)
        );
    }

    @PatchMapping(value = "/update-user/{loginId}")
    public ResponseEntity<ApiResponse> updateUser(@RequestBody UserResponseDTO dto, @RequestHeader String username) {
        UserResponseDTO saved = adminService.updateUser(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "User updated successfully", saved)
        );
    }

    @PostMapping(value = "/role-update")
    public ResponseEntity<String> roleUpdate(@RequestHeader String username, @RequestBody UserResponseDTO dto) throws Exception{
        LOG.info( "Inside roleUpdate : username :{} , dto : {} ", username, dto);
        try {
            Login login = loginRepository.findByUsernameAndIsActive(dto.getUsername(),1);
            if(login!=null) {
                dto.setLoginId(login.getLoginId());
                adminService.updateUser(dto, username);
            }else {
               adminService.addNewUser(dto, username);
            }
            return new ResponseEntity<>("200" , HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("Error fetching userInsert:{} ",e.getMessage());
            return ResponseEntity.status(500).body("Error occurred: " + e.getMessage());
        }
    }


    @GetMapping(value = "/header-module")
    public List<FormModuleDto> headerModule(@RequestParam String roleName) throws Exception {

        return adminService.formModuleList(roleName);
    }

    @GetMapping(value = "/header-detail")
    public List<FormDetailDto> headerDetail(@RequestParam String roleName) throws Exception {

        return adminService.formModuleDetailList(roleName);
    }

    @PostMapping(value = "/form-modules-list")
    public ResponseEntity<List<FormModuleDto>> formModule() throws Exception {

        List<FormModuleDto> list = null;

        try {
            list = adminService.getformModulelist();
        } catch (Exception e) {

            e.printStackTrace();
        }

        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @PostMapping(value = "form-role-access-list")
    public ResponseEntity<List<FormRoleAccessDto>> formRoleAccessList(@RequestBody Map<String, String> request) throws Exception {

        List<FormRoleAccessDto> list = null;

        try {
            String roleName = request.get("roleName");
            String formModuleId = request.get("formModuleId");
            list = adminService.getformRoleAccessList(roleName, formModuleId);
            return new ResponseEntity<>(list, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(List.of(), HttpStatus.EXPECTATION_FAILED);
        }
    }

    @PostMapping(value = "update-form-role-access")
    public String updateFormRoleAccess(@RequestBody FormRoleAccessDto accessDto, @RequestHeader String username) throws Exception {

        String result = null;
        try {
            result = adminService.updateformroleaccess(accessDto, username);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        }
    }

    @GetMapping(value = "/user-login-access")
    public ResponseEntity<Boolean> checkAccess(@RequestHeader String username) {
        LOG.info(" REST request to check access: {}", username);
        try {
            boolean hasAccess = adminService.hasAccess(username);
            return ResponseEntity.ok(hasAccess);
        } catch (Exception e) {
            LOG.error("Error while fetching user access {}", e.getMessage(), e);
            return ResponseEntity.ok(false);
        }
    }

    @GetMapping(value = "/notification-count")
    public ResponseEntity<Integer> getNotificationCount(@RequestHeader String username) throws Exception {
        LOG.info(" Inside get get-notification-count{}", username);
        Integer result = adminService.getNotificationCount(username);
        if (result != null) {
            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }


    @GetMapping(value = "/notification-list")
    public ResponseEntity<List<NotificationDTO>> getNotification(@RequestHeader String username) throws Exception {
        LOG.info(" Inside get get-notification List {}", username);
        List<NotificationDTO> result = adminService.getNotificationList(username);
        if (result != null) {
            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }


    @PutMapping("/update-notification")
    public ResponseEntity<Void> updateNotification(
            @RequestHeader("username") String username,
            @RequestParam("notificationId") String notificationId) {

        LOG.info("Inside update-notification {}", username);

        long result = adminService.updateNotification(username, notificationId);

        if (result != 0) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }


    @PostMapping(value = "/get-role-username")
    public List<Long> getRoleByUsername(@RequestHeader String username) {
        try {
            Login login = loginRepository.findByUsernameAndIsActive(username,1);

            List<Long> roleIds = login.getRoleSecurity().stream()
                    .map(RoleSecurity::getRoleId)
                    .collect(Collectors.toList());

            // You can now store these or perform logic
            LOG.info("Extracted Role IDs for {}: {}", username, roleIds);

            return roleIds; // Returns a JSON array like [1, 2, 3]

        } catch (Exception e) {
            LOG.error("Error processing roles: ", e);
            return new ArrayList<>();
        }
    }

    @GetMapping(value = "/audit-stamping-list")
    public ResponseEntity<List<AuditStampingDTO>> auditStampingList(@RequestHeader String username,
                                                                    @RequestParam String selUser,
                                                                    @RequestParam LocalDate fromDate,
                                                                    @RequestParam LocalDate toDate) throws Exception
    {
        try {

            LOG.info("Inside auditStampingList | User: {},fromDate: {},toDate: {}", username,fromDate,toDate);

            List<AuditStampingDTO> dto = adminService.auditStampingList(selUser, fromDate, toDate);

            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("Error in auditStampingList: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value="custom-audit-stamping-login")
    public String logIn(@RequestBody String username, Authentication authentication, HttpServletRequest request)throws Exception {
        LOG.info(" Inside custom-audit-stamping-login: user:{}, ",username);
        long result=0;
        username = username.replace("\"", "");
        String IpAddress;
        try{
            IpAddress = request.getRemoteAddr();
            if("0:0:0:0:0:0:0:1".equalsIgnoreCase(IpAddress))
            {
                InetAddress ip = InetAddress.getLocalHost();
                IpAddress= ip.getHostAddress();
            }
        }
        catch(Exception e)
        {
            IpAddress="Not Available";
        }
        try{
            Login login = loginRepository.findByUsernameAndIsActive(username,1);
            AuditStamping stamping=new AuditStamping();
            stamping.setLoginId(login.getLoginId());
            stamping.setLoginDate(LocalDate.now());
            stamping.setLoginDatetime(LocalDateTime.now());
            stamping.setUsername(login.getUsername());
            stamping.setIpAddress(IpAddress);
            result = adminService.loginStampingInsert(stamping);
        }catch (Exception e) {
            LOG.error("Error in custom-audit-stamping-login: {}", e.getMessage(), e);
        }
        return String.valueOf(result);

    }


    @PostMapping(value = "custom-audit-stamping-logout")
    public String logout(@RequestBody(required = false) Map<String, Object> requestBody, Authentication authentication) throws Exception {
        LOG.info("Inside custom-auditStamping-logout {}", authentication != null ? authentication.getName() : "Anonymous");
        long result = 0;

        if (requestBody == null) {
            return String.valueOf(result);
        }

        String username = (String) requestBody.get("username");
        String logoutType = (String) requestBody.get("logoutType");

        if (username != null) {
            Login login = loginRepository.findByUsernameAndIsActive(username, 1);
            if (login != null && login.getLoginId() != null) {
                AuditStamping stamping = new AuditStamping();
                stamping.setAuditStampingId(adminService.lastLoginStampingId(login.getLoginId()));
                stamping.setLogoutType(logoutType);
                stamping.setLogoutDateTime(LocalDateTime.now());
                result = adminService.loginStampingUpdate(stamping);
            }
        }
        return String.valueOf(result);
    }

    @GetMapping("/get-login-stats")
    public DashboardLoginStatsDTO getLoginStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return adminService.getLoginStats(startDate, endDate);
    }

    @PutMapping(value = "/change-password")
    public ResponseEntity<String> updatePassword(@RequestHeader String username,@RequestBody ChangePasswordDTO dto){
        LOG.info("REST request to change password for user :{}",username);
        try {
            Integer count = adminService.changePassword(dto);
            if(count > 0){
                return ResponseEntity.ok(count+"");
            }
            return ResponseEntity.badRequest().body("0");

        } catch (Exception e) {
            LOG.error("Error while changing the password for user: {} {}", username, e.getMessage(), e);
            return new ResponseEntity<>("0", HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(value = "/get-license")
    public ResponseEntity<Boolean> getLicense() {
        LOG.info("Inside getLicense()");
        try {
            boolean isValid = adminService.getLicense();
            return ResponseEntity.ok(isValid);
        } catch (Exception e) {
            LOG.error("Error in getLicense: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(false);
        }
    }

    @PutMapping(value = "/update-password")
    public String updatePassword(@RequestHeader String username, @RequestBody ChangePasswordDTO changePasswordDTO, @RequestHeader(value = "Authorization", required = false) String token){
        LOG.info(" Inside Update-password user :{}",username);
        changePasswordDTO.setUsername(username);
        Integer count = adminService.changePasswordForAllApplications(token, username, changePasswordDTO);
        return String.valueOf(count);
    }

    @GetMapping(value = "/handing-over")
    public ResponseEntity<ApiResponse> getHandingOverList(@RequestHeader String username, @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        LOG.info(" Inside getHandingOverList user :{}, fromDate:{}, toDate:{}",username, fromDate, toDate);
        List<HandingOverDTO> list = adminService.getHandingOverList(username, fromDate, toDate);
        return ResponseEntity.ok(new ApiResponse(true, "Handing Over list fetched", list));
    }

    @PostMapping(value = "/add-handing-over")
    public ResponseEntity<ApiResponse> insertHandingOver(@RequestBody HandingOverDTO dto, @RequestHeader String username) {
        if(adminService.existOverlappingDateRange(dto, "insert")){
            return ResponseEntity.ok(
                    new ApiResponse(false, "A handing over record already exists for the selected period.", null)
            );
        }
        HandingOverDTO data = adminService.insertHandingOver(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Handing Over added successfully", data)
        );
    }

    @PutMapping(value = "/update-handing-over")
    public ResponseEntity<ApiResponse> updateHandingOver(@RequestBody HandingOverDTO dto, @RequestHeader String username) {
        if(adminService.existOverlappingDateRange(dto, "update")){
            return ResponseEntity.ok(
                    new ApiResponse(false, "A handing over record already exists for the selected period.", null)
            );
        }
        Optional<HandingOverDTO> data = adminService.updateHandingOver(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Handing Over updated successfully", data)
        );
    }

    @PutMapping(value = "/revoke-handing-over")
    public ResponseEntity<ApiResponse> revokeHandingOver(@RequestParam Long handingOverId, @RequestHeader String username) {
        Optional<HandingOverDTO> data = adminService.revokeHandingOver(handingOverId, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Handing Over revoked successfully", data)
        );
    }

    @PostMapping(value = "/add-cash-limit")
    public ResponseEntity<ApiResponse> addCashLimit(@Valid @RequestBody CashLimit cashLimit,
                                                    @RequestHeader String username) {
        try {
            CashLimit saved = adminService.addCashLimit(cashLimit, username);
            return ResponseEntity.ok(
                    new ApiResponse(true, "Cash Limit added successfully", saved)
            );
        } catch (BadRequestException e) {
            LOG.error("Error while adding Cash Limit", e);
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Failed to add Cash Limit", null)
            );
        }
    }

    @PutMapping(value = "/update-cash-limit/{id}")
    public ResponseEntity<ApiResponse> updateCashLimit(@PathVariable Long id,
                                                       @Valid @RequestBody CashLimit cashLimit,
                                                       @RequestHeader String username) {
        try {
            cashLimit.setCashLimitId(id);
            CashLimit updated = adminService.updateCashLimit(cashLimit, username);
            return ResponseEntity.ok(
                    new ApiResponse(true, "Cash Limit updated successfully", updated)
            );
        } catch (Exception e) {
            LOG.error("Error while updating Cash Limit", e);
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Failed to update Cash Limit", null)
            );
        }
    }

    @GetMapping(value = "/cash-limit-list")
    public ResponseEntity<ApiResponse> getCashLimitList() {

        try {
            List<CashLimit> cashLimitList = adminService.getCashLimitList();
            return ResponseEntity.ok(
                    new ApiResponse(true, "Cash Limit List fetched successfully", cashLimitList)
            );
        } catch (Exception e) {
            LOG.error("Error while fetching Cash Limit List", e);
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Failed to fetch Cash Limit List", null)
            );
        }
    }

}
