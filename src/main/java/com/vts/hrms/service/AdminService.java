package com.vts.hrms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vts.hrms.dto.*;
import com.vts.hrms.entity.*;
import com.vts.hrms.exception.BadRequestException;
import com.vts.hrms.exception.NotFoundException;
import com.vts.hrms.mapper.HandingOverMapper;
import com.vts.hrms.repository.*;
import com.vts.hrms.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    private final RoleRepository roleRepository;
    private final LoginRepository loginRepository;
    private final RoleSecurityRepository roleSecurityRepository;
    private final MasterClientService masterClient;
    private final FormModuleRepository formModuleRepository;
    private final FormDetailRepository formDetailRepository;
    private final FormRoleAccessRepository formRoleAccessRepository;
    private final NotificationRepository notificationRepository;
    private final MasterCacheService masterCacheService;
    private final AuditStampingRepository auditStampingRepository;
    private final HandingOverRepository handingOverRepository;
    private final HandingOverMapper handingOverMapper;
    private final CashLimitRepository cashLimitRepository;


    @Value("${x_api_key}")
    private String xApiKey;

    @Value("${labCode}")
    private String labCode;

    @Value("${license}")
    private String license;

    private DateTimeFormatter formatter;

    public AdminService(RoleRepository roleRepository, LoginRepository loginRepository, RoleSecurityRepository roleSecurityRepository, MasterClientService masterClient, FormModuleRepository formModuleRepository, FormDetailRepository formDetailRepository, FormRoleAccessRepository formRoleAccessRepository, NotificationRepository notificationRepository, MasterCacheService masterCacheService, AuditStampingRepository auditStampingRepository, HandingOverRepository handingOverRepository, HandingOverMapper handingOverMapper, CashLimitRepository cashLimitRepository) {
        this.roleRepository = roleRepository;
        this.loginRepository = loginRepository;
        this.roleSecurityRepository = roleSecurityRepository;
        this.masterClient = masterClient;
        this.formModuleRepository = formModuleRepository;
        this.formDetailRepository = formDetailRepository;
        this.formRoleAccessRepository = formRoleAccessRepository;
        this.notificationRepository = notificationRepository;
        this.masterCacheService = masterCacheService;
        this.auditStampingRepository = auditStampingRepository;
        this.handingOverRepository = handingOverRepository;
        this.handingOverMapper = handingOverMapper;
        this.cashLimitRepository = cashLimitRepository;
    }

    @Cacheable(value = "roleList")
    public List<RoleDTO> getRoleList() {
        log.info("Fetching all roles");
        return roleRepository
                .findAll().stream()
                .map(data -> {
                    RoleDTO dto = new RoleDTO();
                    dto.setRoleId(data.getRoleId());
                    dto.setRoleName(data.getRoleName());
                    return dto;
                }).toList();
    }

    @Cacheable(value = "userList")
    public List<UserResponseDTO> getUserList() {
        log.info("Fetching all users");
        List<UserListRowDTO> rows = loginRepository.getUserList();

        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();

        // group flat rows (one per role) by loginId
        Map<Long, UserResponseDTO> grouped = new LinkedHashMap<>();

        for (UserListRowDTO row : rows) {
            UserResponseDTO user = grouped.get(row.getLoginId());

            if (user == null) {
                user = new UserResponseDTO();
                user.setLoginId(row.getLoginId());
                user.setEmpId(row.getEmpId());
                user.setDivisionId(row.getDivisionId());
                user.setUsername(row.getUsername());
                user.setRoleIds(new ArrayList<>());
                user.setRoleNames(new ArrayList<>());

                EmployeeDTO employee = employeeMap.get(row.getEmpId());
                if (employee != null) {
                    user.setEmployeeName(CommonUtil.buildEmployeeName(employee, false));
                    user.setDesignationName(employee.getEmpDesigName());
                    user.setDivisionName(employee.getEmpDivCode());
                }

                grouped.put(row.getLoginId(), user);
            }

            if (row.getRoleId() != null && !user.getRoleIds().contains(row.getRoleId())) {
                user.getRoleIds().add(row.getRoleId());
            }
            if (row.getRoleName() != null && !user.getRoleNames().contains(row.getRoleName())) {
                user.getRoleNames().add(row.getRoleName());
            }
        }

        return new ArrayList<>(grouped.values());
    }

    public boolean checkUsernameExists(String username) {
        log.info("Request to check username : {}", username);
        return loginRepository.existsByUsernameIgnoreCase(username);
    }

    public UserResponseDTO getUserById(Long loginId) {
        log.info("Fetching user by loginId: {}", loginId);

        Login login = loginRepository.findById(loginId)
                .orElseThrow(() -> new RuntimeException("Login not found for loginId: " + loginId));

        EmployeeDTO employee = masterCacheService.getLongEmployeeDTOMap().get(login.getEmpId());

        List<Long> roleIds = login.getRoleSecurity().stream()
                .map(RoleSecurity::getRoleId)
                .collect(Collectors.toList());

        List<String> roleNames = login.getRoleSecurity().stream()
                .map(RoleSecurity::getRoleName)
                .collect(Collectors.toList());

        Map<String, String> roleHindiMap = roleRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        Role::getRoleName,
                        Role::getHindiRoleName
                ));

        List<String> hindiRoleNames = roleNames.stream()
                .map(roleHindiMap::get)
                .toList();

        return new UserResponseDTO(
                login.getLoginId(),
                login.getEmpId(),
                employee != null ? employee.getDivisionId() : null,
                login.getUsername(),
                employee != null ? CommonUtil.buildEmployeeName(employee, false) : null,
                employee != null ? employee.getEmpDesigName() : null,
                employee != null ? employee.getEmpDivCode() : null,
                roleIds,
                roleNames,
                hindiRoleNames
        );

    }

    @CacheEvict(value = "userList", allEntries = true)
    @Transactional
    public UserResponseDTO addNewUser(UserResponseDTO dto, String username) {

        log.info("Adding new user for username: {}", dto.getUsername());

        if (dto.getRoleIds() == null || dto.getRoleIds().isEmpty()) {
            throw new BadRequestException("At least one role must be selected");
        }

        try {
            String formattedLabCode = labCode.substring(0, 1).toUpperCase() + labCode.substring(1).toLowerCase();
            String password = formattedLabCode + "@123";

            // 1. Fetch all selected roles
            List<RoleSecurity> roleList = roleSecurityRepository.findAllById(dto.getRoleIds());
            if (roleList.size() != dto.getRoleIds().size()) {
                throw new RuntimeException("One or more roleIds not found");
            }

            // 2. Create Login Object
            Login login = new Login();
            login.setUsername(dto.getUsername());
            login.setEmpId(dto.getEmpId());
            login.setPassword(encoder.encode(password));
            login.setCreatedBy(username);
            login.setCreatedDate(LocalDateTime.now());
            login.setIsActive(1);

            // 3. Map Roles ↔ Login (many-to-many)
            Set<RoleSecurity> roleSet = new HashSet<>(roleList);
            login.setRoleSecurity(roleSet);

            // 4. Save Login (join table auto insert for each role)
            Login savedLogin = loginRepository.save(login);

            return new UserResponseDTO(
                    savedLogin.getLoginId(),
                    savedLogin.getEmpId(),
                    dto.getDivisionId(),
                    savedLogin.getUsername(),
                    dto.getEmployeeName(),
                    dto.getDesignationName(),
                    dto.getDivisionName(),
                    roleList.stream().map(RoleSecurity::getRoleId).collect(Collectors.toList()),
                    roleList.stream().map(RoleSecurity::getRoleName).collect(Collectors.toList()),
                    null
            );

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error while adding user", e);
            throw new BadRequestException("Failed to add new user");
        }
    }

    @CacheEvict(value = "userList", allEntries = true)
    @Transactional
    public UserResponseDTO updateUser(UserResponseDTO dto, String username) {

        log.info("Updating user with loginId: {}", dto.getLoginId());

        if (dto.getRoleIds() == null || dto.getRoleIds().isEmpty()) {
            throw new BadRequestException("At least one role must be selected");
        }

        try {
            // 1. Fetch existing Login
            Login login = loginRepository.findById(dto.getLoginId())
                    .orElseThrow(() -> new RuntimeException(
                            "Login not found for loginId: " + dto.getLoginId()));

            // 2. Fetch all selected roles
            List<RoleSecurity> roleList = roleSecurityRepository.findAllById(dto.getRoleIds());
            if (roleList.size() != dto.getRoleIds().size()) {
                throw new RuntimeException("One or more roleIds not found");
            }

            // 3. Update login fields
            login.setUsername(dto.getUsername());
            login.setEmpId(dto.getEmpId());
            login.setModifiedBy(username);
            login.setModifiedDate(LocalDateTime.now());

            // 4. Replace role mapping (join table) with new full set
            login.getRoleSecurity().clear();
            login.getRoleSecurity().addAll(roleList);

            // 5. Save changes
            Login updatedLogin = loginRepository.save(login);

            return new UserResponseDTO(
                    updatedLogin.getLoginId(),
                    updatedLogin.getEmpId(),
                    dto.getDivisionId(),
                    updatedLogin.getUsername(),
                    dto.getEmployeeName(),
                    dto.getDesignationName(),
                    dto.getDivisionName(),
                    roleList.stream().map(RoleSecurity::getRoleId).collect(Collectors.toList()),
                    roleList.stream().map(RoleSecurity::getRoleName).collect(Collectors.toList()),
                    null

            );

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error while updating user", e);
            throw new RuntimeException("Failed to update user");
        }
    }


    @Cacheable(value = "formModuleListByRole", key = "#roleName")
    public List<FormModuleDto> formModuleList(String roleName) throws Exception {
        log.info(" Inside formModuleList ");
        try {
            List<FormModuleDto> formModuleDtoList = new ArrayList<>();
            List<FormModule> formModuleList = formModuleRepository.findDistinctFormModulesByRoleId(roleName);

            formModuleList.forEach(detail -> {
                FormModuleDto formModuleDto = FormModuleDto.builder()
                        .FormModuleId(detail.getFormModuleId())
                        .FormModuleName(detail.getFormModuleName())
                        .hindiFormModuleName(detail.getHindiFormModuleName())
                        .ModuleUrl(detail.getModuleUrl())
                        .ModuleIcon(detail.getModuleIcon())
                        .SerialNo(detail.getSerialNo())
                        .IsActive(detail.getIsActive())
                        .build();

                formModuleDtoList.add(formModuleDto);
            });

            return formModuleDtoList;
        } catch (Exception e) {
            log.error(" Inside formModuleList ", e);

            return new ArrayList<FormModuleDto>();
        }
    }


    @Cacheable(value = "formModuleList")
    public List<FormModuleDto> getformModulelist() throws Exception {
        log.info(" AdminServiceImpl Inside method getformModulelist ");
        List<FormModuleDto> FMlist = new ArrayList<FormModuleDto>();
        try {

            List<Object[]> list = formModuleRepository.getformModulelist();
            if (list != null) {
                for (Object[] O : list) {
                    FormModuleDto dto = new FormModuleDto();
                    dto.setFormModuleId(Long.parseLong(O[0].toString()));
                    dto.setFormModuleName(O[1].toString());
                    dto.setHindiFormModuleName(O[2].toString());
                    FMlist.add(dto);
                }
            } else {
                FMlist = null;
            }
        } catch (Exception e) {
            log.error(" error in AdminServiceImpl Inside method getformModulelist " + e.getMessage());
            e.printStackTrace();
        }

        return FMlist;
    }


    @Cacheable(value = "formModuleDetailListByRole", key = "#roleName")
    public List<FormDetailDto> formModuleDetailList(String roleName) throws Exception {
        log.info(" Inside formModuleDetailList ");
        try {
            List<FormDetailDto> formDetailDtoList = new ArrayList<>();
            List<FormDetail> formDetailList = formDetailRepository.findDistinctFormModulesDetailsByRoleId(roleName);

            formDetailList.forEach(detail -> {
                FormDetailDto formModuleDto = FormDetailDto.builder()
                        .FormDetailId(detail.getFormDetailId())
                        .FormModuleId(detail.getFormModuleId())
                        .FormName(detail.getFormName())
                        .FormUrl(detail.getFormUrl())
                        .FormDispName(detail.getFormDispName())
                        .hindiFormDispName(detail.getHindiFormDispName())
                        .FormSerialNo(detail.getFormSerialNo())
                        .FormColor(detail.getFormColor())
                        .ModifiedBy(detail.getModifiedBy())
                        .ModifiedDate(detail.getModifiedDate())
                        .IsActive(detail.getIsActive())
                        .build();

                formDetailDtoList.add(formModuleDto);
            });

            return formDetailDtoList;
        } catch (Exception e) {
            log.error(" Inside formModuleDetailList ", e);
            e.printStackTrace();
            return new ArrayList<FormDetailDto>();
        }
    }


    @Cacheable(value = "formRoleAccessListByRole", key = "#roleId + '_' + #formModuleId")
    public List<FormRoleAccessDto> getformRoleAccessList(String roleId, String formModuleId) {
        log.info(" AdminServiceImpl Inside method getformRoleAccessList");
        try {

            List<Object[]> list = formRoleAccessRepository.getformroleAccessList(roleId, formModuleId);
            return list.stream().map(row -> {
                return FormRoleAccessDto.builder()
                        .formRoleAccessId(row[0] != null ? Long.parseLong(row[0].toString()) : 0L)
                        .formDetailId(row[1] != null ? Long.parseLong(row[1].toString()) : 0L)
                        .formModuleId(row[2] != null ? Long.parseLong(row[2].toString()) : 0L)
                        .formDispName(row[3] != null ? row[3].toString() : null)
                        .isActive(row[4] != null && row[4].toString().equalsIgnoreCase("1"))
                        .forView(row[5] != null && row[5].toString().equalsIgnoreCase("Y"))
                        .forAdd(row[6] != null && row[6].toString().equalsIgnoreCase("Y"))
                        .forEdit(row[7] != null && row[7].toString().equalsIgnoreCase("Y"))
                        .forDelete(row[8] != null && row[8].toString().equalsIgnoreCase("Y"))
                        .roleId(row[9] != null ? Long.parseLong(row[9].toString()) : 0)
                        .build();
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error(" error in AdminServiceImpl Inside method getformRoleAccessList " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    @CacheEvict(value = {"formRoleAccessListByRole", "formModuleListByRole",
            "formModuleDetailListByRole"}, allEntries = true)
    @Transactional
    public String updateformroleaccess(FormRoleAccessDto accessDto, String username) {
        log.info(" AdminServiceImpl Inside method updateformroleaccess");
        String updateResult = null;
        try {
            long result = formRoleAccessRepository.countByFormRoleIdAndDetailId(String.valueOf(accessDto.getRoleId()), String.valueOf(accessDto.getFormDetailId()));
            if (result == 0) {
                FormRoleAccess formrole = new FormRoleAccess();
                formrole.setRoleId(accessDto.getRoleId());
                formrole.setFormDetailId(accessDto.getFormDetailId());
                formrole.setIsActive(1);
                formrole.setForView(String.valueOf(accessDto.isForView()).equalsIgnoreCase("true") ? "Y" : "N");
                formrole.setForAdd(String.valueOf(accessDto.isForAdd()).equalsIgnoreCase("true") ? "Y" : "N");
                formrole.setForEdit(String.valueOf(accessDto.isForEdit()).equalsIgnoreCase("true") ? "Y" : "N");
                formrole.setForDelete(String.valueOf(accessDto.isForDelete()).equalsIgnoreCase("true") ? "Y" : "N");
                formrole.setCreatedBy(username);
                formrole.setCreatedDate(LocalDateTime.now());
                formRoleAccessRepository.save(formrole);
                updateResult = String.valueOf(formrole.getFormRoleAccessId());
            } else {
                Optional<FormRoleAccess> formRoleAccess = formRoleAccessRepository.findById(accessDto.getFormRoleAccessId());
                if (formRoleAccess.isPresent()) {
                    FormRoleAccess roleAccess = formRoleAccess.get();
//                    roleAccess.setIsActive(String.valueOf(accessDto.isActive()).equalsIgnoreCase("true") ? 1 : 0);
                    roleAccess.setForView(String.valueOf(accessDto.isForView()).equalsIgnoreCase("true") ? "Y" : "N");
                    roleAccess.setForAdd(String.valueOf(accessDto.isForAdd()).equalsIgnoreCase("true") ? "Y" : "N");
                    roleAccess.setForEdit(String.valueOf(accessDto.isForEdit()).equalsIgnoreCase("true") ? "Y" : "N");
                    roleAccess.setForDelete(String.valueOf(accessDto.isForDelete()).equalsIgnoreCase("true") ? "Y" : "N");
                    roleAccess.setModifiedBy(username);
                    roleAccess.setModifiedDate(LocalDateTime.now());
                    formRoleAccessRepository.save(roleAccess);
                    updateResult = String.valueOf(roleAccess.getFormRoleAccessId());
                }
            }
            return updateResult;
        } catch (Exception e) {
            log.error(" error in AdminServiceImpl Inside method updateformroleaccess " + e.getMessage());
            e.printStackTrace();
            return "0";
        }
    }

    public boolean hasAccess(String username) {
        try {
            return loginRepository.existsByUsernameAndIsActive(username, 1);
        } catch (Exception e) {
            return false;
        }
    }

    public Integer getNotificationCount(String username) {
        Login login = loginRepository.findByUsernameAndIsActive(username, 1);
        int count = 0;
        try {
            count = notificationRepository.getNotificationCount(login.getEmpId());
        } catch (Exception e) {
            log.error("AuditServiceImpl Inside method getNotificationCount(){}", String.valueOf(e));
        }
        return count;
    }

    @Cacheable(value = "notificationList", key = "#username")
    public List<NotificationDTO> getNotificationList(String username) {
        log.info("Inside method getNotificationList ");
        Login login = loginRepository.findByUsernameAndIsActive(username, 1);

//        List<EmployeeDTO> empData = masterClient.getEmployee(xApiKey, login.getEmpId());
//        EmployeeDTO eDto = !empData.isEmpty() ? empData.get(0) : new EmployeeDTO();

        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();

        List<Notification> notificationList = notificationRepository.getNotificationList(login.getEmpId());
        return notificationList.stream()
                .map(data -> {

                    EmployeeDTO employeeDTO = employeeMap.get(data.getNotificationBy());

                    return NotificationDTO.builder()
                            .notificationId(data.getNotificationId())
                            .empName(CommonUtil.buildEmployeeName(employeeDTO, true))
//                            .empDesig(eDto.getEmpDesigCode())
                            .notificationMessage(data.getNotificationMessage())
                            .notificationDate(data.getNotificationDate())
                            .notificationUrl(data.getNotificationUrl())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "notificationList", allEntries = true)
    public long updateNotification(String username, String notificationId) {
        log.info("Inside method updateNotification ");
        try {
            Optional<Notification> notifOptional = notificationRepository.findById(Long.parseLong(notificationId));

            if (notifOptional.isPresent()) {
                // Get the notification object from the Optional
                Notification notification = notifOptional.get();

                // Update the necessary fields
                notification.setModifiedBy(username);
                notification.setModifiedDate(LocalDateTime.now());
                notification.setIsActive(0);

                // Save the updated entity back to the repository
                Notification updatedNotification = notificationRepository.save(notification);

                // Return the ID of the updated notification
                return updatedNotification.getNotificationId();
            } else {
                log.error("Notification with ID {} not found.", notificationId);
                throw new Exception("Notification not found");
            }
        } catch (Exception e) {
            log.error("Error in updateNotification: {}", e.getMessage(), e);
            return 0;
        }
    }

    public List<AuditStampingDTO> auditStampingList(String username, LocalDate fromDate, LocalDate toDate) {

        if (fromDate == null || toDate == null) {
            log.warn("auditStampingList : One or more required parameters are null - fromDate : {}, toDate : {}", fromDate, toDate);
            return List.of();
        }

        log.info("LoginService Inside method auditStampingList | user: {}, fromDate : {}, toDate : {}", username, fromDate, toDate);

        try {
            return auditStampingRepository.auditList(username, fromDate, toDate.plusDays(1));

        } catch (Exception e) {
            log.error("Error in LoginService Inside method auditStampingList: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public DashboardLoginStatsDTO getLoginStats(String startDate, String endDate) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Calculate standard timeframes
        LocalDateTime start24h = now.minusHours(24);
        LocalDateTime startWeek = now.minusWeeks(1L);
        LocalDateTime startMonth = now.minusMonths(1L);

        // 2. Parse Custom Dates
        LocalDateTime customStart = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime customEnd = LocalDate.parse(endDate).atTime(23, 59, 59);

        // 3. Fetch all counts (These are fast indexed counts)
        Long c24h = auditStampingRepository.countLogins(start24h, now);
        Long cWeek = auditStampingRepository.countLogins(startWeek, now);
        Long cMonth = auditStampingRepository.countLogins(startMonth, now);
        Long active = auditStampingRepository.countActiveNow();

        // 4. Fetch Custom Range Data (Count + Chart)
        Long customTotal = auditStampingRepository.countLogins(customStart, customEnd);
        List<LoginChartPointDTO> customChart = mapDaily(
                auditStampingRepository.groupDaily(customStart, customEnd)
        );

        // 5. Return Unified DTO
        // Structure: appCode, count24h, countWeek, countMonth, activeNow, customTotal, customChart
        return new DashboardLoginStatsDTO(
                "HRMS",
                c24h,
                cWeek,
                cMonth,
                active,
                customTotal,
                customChart
        );
    }

    private List<LoginChartPointDTO> mapDaily(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new LoginChartPointDTO(r[0].toString(), ((Number) r[1]).longValue()))
                .toList();
    }

    public long loginStampingInsert(AuditStamping stamping) throws Exception {
        long result = 0;
        if (stamping == null) {
            log.warn("loginStampingInsert : One required parameters is null - stamping :{}", stamping);
            return result;
        }
        log.info("AdminService Inside method loginStampingInsert");
        try {
            AuditStamping audit = auditStampingRepository.save(stamping);
            if (audit.getAuditStampingId() != null) {
                return 1;
            }
        } catch (Exception e) {
            log.error("error in AdminService Inside method loginStampingInsert:{} ", e.getMessage(), e);
        }
        return result;
    }

    public Long lastLoginStampingId(Long loginId) throws Exception {
        if (loginId == null) {
            log.warn("lastLoginStampingId : One required parameter is null - loginId :{}", loginId);
            return 0L;
        }

        log.info("AdminService Inside method LastLoginStampingId : loginId :{}", loginId);

        try {
            Optional<Long> result = auditStampingRepository.findLastLoginStampingId(loginId);

            // Convert the Long result to String safely
            return result.orElse(0L);

        } catch (Exception e) {
            log.error("Error in AdminService Inside method LastLoginStampingId :{}", e.getMessage(), e);
            throw new Exception("Error while fetching last login stamping ID", e);
        }
    }

    public long loginStampingUpdate(AuditStamping stamping) throws Exception {
        long result = 0;
        if (stamping == null) {
            log.warn("loginStampingUpdate : One required parameters is null - stamping ");
            return result;
        }
        log.info("AdminService Inside method LoginStampingUpdate {}", stamping);
        try {
            Optional<AuditStamping> prevStampingDetails = auditStampingRepository.findById(stamping.getAuditStampingId());
            if (prevStampingDetails.isPresent()) {
                AuditStamping auditStamping = prevStampingDetails.get();
                auditStamping.setAuditStampingId(stamping.getAuditStampingId());
                auditStamping.setLogoutType(stamping.getLogoutType());
                auditStamping.setLogoutDateTime(stamping.getLogoutDateTime());
                auditStampingRepository.save(auditStamping);
                return 1;
            }
        } catch (Exception e) {
            log.error(" error in AdminService Inside method LoginStampingUpdate {}", e.getMessage(), e);
        }
        return result;
    }

    public Integer changePassword(ChangePasswordDTO changePasswordDTO) {
        String username = changePasswordDTO.getUsername();
        String oldPassword = changePasswordDTO.getOldPassword();
        String newPassword = changePasswordDTO.getNewPassword();

        if (username == null || oldPassword == null || newPassword == null) {
            log.warn("changePassword: One of Required parameter is null or empty, username = {}", username);
            return 0;
        }
        log.info("Inside Update-Password: username = {}", username);
        try {
            Login login = loginRepository.findByUsername(username);
            String actualOldPassword = login.getPassword();

            // 1. Old and new password should not be same
            if (oldPassword.equals(newPassword)) {
                return 422;
            }

            // 2. Old password does not match DB password
            if (!encoder.matches(oldPassword, actualOldPassword)) {
                return 401;
            }

            if (encoder.matches(oldPassword, actualOldPassword)) {
                String encodedNewPassword = encoder.encode(newPassword);
                login.setPassword(encodedNewPassword);
                login.setModifiedBy(username);
                login.setModifiedDate(LocalDateTime.now());
                loginRepository.save(login);
                return 200;
            }
            return 0;
        } catch (Exception e) {
            log.error("Exception in Update-Password for username {}: {}", username, e.getMessage(), e);
            return 400;
        }
    }


    public Boolean getLicense() {
        try {
            if (license == null || license.isBlank()) {
                log.warn("License token is empty");
                return false;
            }

            String[] parts = license.split("\\.");

            if (parts.length < 2) {
                log.warn("Invalid JWT format");
                return false;
            }

            String payloadJson =
                    new String(Base64.getUrlDecoder().decode(parts[1]));

            ObjectMapper mapper = new ObjectMapper();
            JsonNode payload = mapper.readTree(payloadJson);

            if (!payload.has("exp")) {
                log.warn("JWT does not contain exp");
                return false;
            }

            long exp = payload.get("exp").asLong();
            Instant expiryInstant = Instant.ofEpochSecond(exp);
            return expiryInstant.isAfter(Instant.now());
        } catch (Exception e) {
            log.error("License validation failed", e);
            return false;
        }
    }

    public Integer changePasswordForAllApplications(String token, String username, ChangePasswordDTO changePasswordDTO) {
        log.info(" Inside LoginService changePasswordForAllApplications: {} ", username);
        try {
            return masterClient.changePassword(token, username, changePasswordDTO);
        } catch (Exception e) {
            log.error("Error while changing the password: {}", e.getMessage(), e);
            return 400;
        }
    }

    public List<HandingOverDTO> getHandingOverList(String username, LocalDate fromDate, LocalDate toDate) {
        log.info("Request to getHandingOverList list by username {}", username);
        List<HandingOverDTO> list = handingOverRepository
                .findAllByFromDateBetweenOrderByHandingOverIdDesc(fromDate, toDate)
                .stream()
                .map(handingOverMapper::toDto)
                .toList();

        if (list.isEmpty()) {
            return list;
        }

        Map<Long, EmployeeDTO> employeeMap = masterCacheService.getLongEmployeeDTOMap();

        for (HandingOverDTO dto : list) {

            EmployeeDTO fromEmp = employeeMap.get(dto.getFromEmpId());
            EmployeeDTO toEmp = employeeMap.get(dto.getToEmpId());
            if (fromEmp != null) {
                dto.setFromEmpName(CommonUtil.buildEmployeeName(fromEmp, true));
            }
            if (toEmp != null) {
                dto.setToEmpName(CommonUtil.buildEmployeeName(toEmp, true));
            }
        }
        System.out.println(list.toString());
        return list;
    }

    public boolean existOverlappingDateRange(HandingOverDTO dto, String action) {
        Long handingOverId = action.equalsIgnoreCase("update") ? dto.getHandingOverId() : null;
        return handingOverRepository.existsOverlappingDateRange(handingOverId, dto.getFromEmpId(), dto.getFromDate(), dto.getToDate());
    }

    public HandingOverDTO insertHandingOver(HandingOverDTO dto, String username) {

        HandingOver handingOver = handingOverMapper.toEntity(dto);
        handingOver.setCreatedBy(username);
        handingOver.setCreatedDate(LocalDateTime.now());
        handingOver.setIsActive(1);
        handingOver = handingOverRepository.save(handingOver);
        return handingOverMapper.toDto(handingOver);
    }

    public Optional<HandingOverDTO> updateHandingOver(HandingOverDTO dto, String username) {
        log.info("Request to update Handing Over for id {} by {}", dto.getHandingOverId(), username);
        return handingOverRepository
                .findById(dto.getHandingOverId())
                .map(existingData -> {
                    existingData.setModifiedBy(username);
                    existingData.setModifiedDate(LocalDateTime.now());
                    handingOverMapper.partialUpdate(existingData, dto);
                    return existingData;
                })
                .map(handingOverRepository::save)
                .map(handingOverMapper::toDto);
    }


    public Optional<HandingOverDTO> revokeHandingOver(Long handingOverId, String username) {
        log.info("Request to revoke Handing Over for id {} by {}", handingOverId, username);

        return handingOverRepository.findById(handingOverId)
                .map(existingData -> {
                    existingData.setModifiedBy(username);
                    existingData.setModifiedDate(LocalDateTime.now());
                    existingData.setIsActive(0);

                    return existingData;
                })
                .map(handingOverRepository::save)
                .map(handingOverMapper::toDto);
    }

    @CacheEvict(value = "cashLimitList", allEntries = true)
    @Transactional
    public CashLimit addCashLimit(CashLimit cashLimit, String username) {
        log.info("Adding Cash Limit");
        log.info("Received Cash Limit: {}", cashLimit.getCashLimit());

        if (cashLimit.getCashLimit() == null ||
                cashLimit.getCashLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Cash Limit must be greater than zero");
        }
        CashLimit previousRecord = cashLimitRepository
                .findTopByIsActiveOrderByCashLimitIdDesc(1)
                .orElseThrow(() -> new NotFoundException("Cash limit is not configured."));

        if (previousRecord == null) {
            // First record
            if (cashLimit.getFromDate() == null) {
                throw new BadRequestException("From Date is required");
            }
        } else {
            // Existing records
            LocalDate expectedFromDate =
                    previousRecord.getToDate().plusDays(1);
            cashLimit.setFromDate(expectedFromDate);
        }

        if (cashLimit.getToDate() == null) {
            throw new BadRequestException("To Date is required");
        }

        if (cashLimit.getToDate().isBefore(cashLimit.getFromDate())) {
            throw new BadRequestException(
                    "To Date cannot be before From Date"
            );
        }

        try {
            cashLimitRepository.deactivateActiveRecords();
            cashLimit.setCreatedBy(username);
            cashLimit.setCreatedDate(LocalDateTime.now());
            cashLimit.setIsActive(1);
            cashLimit.setFromDate(cashLimit.getFromDate());
            cashLimit.setToDate(cashLimit.getToDate());

            return cashLimitRepository.save(cashLimit);

        } catch (Exception e) {
            log.error("Error while adding Cash Limit", e);
            throw new BadRequestException("Failed to add Cash Limit");
        }
    }

    @CacheEvict(value = "cashLimitList", allEntries = true)
    @Transactional
    public CashLimit updateCashLimit(CashLimit cashLimit, String username) {
        log.info("Updating Cash Limit : {}", cashLimit.getCashLimitId());

        if (cashLimit.getCashLimit() == null ||
                cashLimit.getCashLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Cash Limit must be greater than zero");
        }

        if (cashLimit.getFromDate() == null) {
            throw new BadRequestException("From Date is required");
        }

        if (cashLimit.getToDate() == null) {
            throw new BadRequestException("To Date is required");
        }

        if (cashLimit.getToDate().isBefore(cashLimit.getFromDate())) {
            throw new BadRequestException("To Date cannot be before From Date");
        }

        CashLimit existing = cashLimitRepository.findById(cashLimit.getCashLimitId())
                .orElseThrow(() -> new RuntimeException("Cash Limit not found"));

        existing.setCashLimit(cashLimit.getCashLimit());
        existing.setFromDate(cashLimit.getFromDate());
        existing.setToDate(cashLimit.getToDate());
        existing.setModifiedBy(username);
        existing.setModifiedDate(LocalDateTime.now());

        return cashLimitRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<CashLimit> getCashLimitList() {
        log.info("Fetching Cash Limit List");

        try {
            return cashLimitRepository.findAllByOrderByCashLimitIdDesc();

        } catch (Exception e) {
            log.error("Error while fetching Cash Limit List", e);
            throw new NotFoundException("Failed to fetch Cash Limit List");
        }
    }

}



