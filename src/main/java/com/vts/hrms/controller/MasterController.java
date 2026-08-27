package com.vts.hrms.controller;

import com.vts.hrms.dto.*;
import com.vts.hrms.service.MasterClientService;
import com.vts.hrms.service.MasterService;
import com.vts.hrms.util.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/master")
public class MasterController {

    private final MasterService masterService;
    private final MasterClientService masterClient;

    @Value("${x_api_key}")
    private String xApiKey;

    @Value("${labCode}")
    private String labCode;

    public MasterController(MasterService masterService, MasterClientService masterClient) {
        this.masterService = masterService;
        this.masterClient = masterClient;
    }

    @GetMapping(value = "/designation")
    public ResponseEntity<ApiResponse> getAllDesignation() {
        List<DesignationDTO> list = masterService.getEmpDesigMaster();

        return ResponseEntity.ok(
                new ApiResponse(true, "Designation list fetched", list)
        );
    }

    @GetMapping(value = "/division")
    public ResponseEntity<ApiResponse> getAllDivision() {
        List<DivisionDTO> list = masterService.getDivisionMaster();

        return ResponseEntity.ok(
                new ApiResponse(true, "Division list fetched", list)
        );
    }

    @GetMapping("/getUser/{username}")
    public ResponseEntity<ApiResponse> getEmployeeByUsername(@PathVariable String username) {
        LoginEmployeeDto employee = masterService.getEmployeeByUsername(username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Employee fetched by username", employee)
        );
    }

    @GetMapping("/employee")
    public ResponseEntity<ApiResponse> getAllEmployees(@RequestParam Long empId, @RequestParam String roleName) {

        Comparator<EmployeeDTO> employeeComparator =
                Comparator.comparingLong(
                        employee -> employee.getSrNo() == 0
                                ? Long.MAX_VALUE
                                : employee.getSrNo()
                );

        // Common filter: Only Present/Active employees
        List<EmployeeDTO> activeEmployees = masterService.getEmployeeList()
                .stream()
                .filter(Objects::nonNull)
                .filter(employee ->
                        "P".equalsIgnoreCase(employee.getEmpStatus())
                )
                .toList();

        List<EmployeeDTO> list;

        if ("ROLE_ADMIN".equalsIgnoreCase(roleName)) {

            list = activeEmployees.stream()
                    .sorted(employeeComparator)
                    .toList();

        } else if ("ROLE_USER".equalsIgnoreCase(roleName)) {

            list = activeEmployees.stream()
                    .filter(employee -> Objects.equals(employee.getEmpId(), empId))
                    .sorted(employeeComparator)
                    .toList();

        } else if ("ROLE_DH".equalsIgnoreCase(roleName)) {

            Set<Long> divisionIds = masterService.getDivisionMaster()
                    .stream()
                    .filter(division ->
                            Objects.equals(division.getDivisionHeadId(), empId)
                    )
                    .map(DivisionDTO::getDivisionId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            list = activeEmployees.stream()
                    .filter(employee ->
                            divisionIds.contains(employee.getDivisionId())
                    )
                    .sorted(employeeComparator)
                    .toList();

        } else if ("ROLE_GH".equalsIgnoreCase(roleName)) {

            List<DivisionDTO> divisions = masterService.getDivisionMaster();

            Set<Long> divisionIds;

            if ("CAIR".equalsIgnoreCase(labCode)) {

                divisionIds = divisions.stream()
                        .filter(division ->
                                Objects.equals(
                                        division.getDivisionHeadId(),
                                        empId
                                )
                        )
                        .map(DivisionDTO::getDivisionId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

            } else {

                Optional<DivisionGroupDTO> groupOpt =
                        masterClient
                                .getDivisionGroupMasterList(xApiKey)
                                .stream()
                                .filter(group ->
                                        Objects.equals(
                                                group.getGroupHeadId(),
                                                empId
                                        )
                                )
                                .findFirst();

                Long groupId = groupOpt
                        .map(DivisionGroupDTO::getGroupId)
                        .orElse(null);

                divisionIds = divisions.stream()
                        .filter(division ->
                                Objects.equals(
                                        division.getGroupId(),
                                        groupId
                                )
                        )
                        .map(DivisionDTO::getDivisionId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }

            list = activeEmployees.stream()
                    .filter(employee ->
                            divisionIds.contains(employee.getDivisionId())
                    )
                    .sorted(employeeComparator)
                    .toList();

        } else {
            list = Collections.emptyList();
        }

        return ResponseEntity.ok(
                new ApiResponse(
                        true,
                        "Employee list fetched successfully",
                        list
                )
        );
    }

    @GetMapping(value = "/sign-auth-roles")
    public ResponseEntity<ApiResponse> getSignAuthRoles(@RequestHeader String username) {
        List<SignAuthRoleDTO> list = masterService.getSignAuthRoles(username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Sign Auth Role list fetched", list)
        );
    }

    @GetMapping(value = "/sign-authority")
    public ResponseEntity<ApiResponse> getSignAuthorities(@RequestHeader String username) {
        List<SignRoleAuthorityDTO> list = masterService.getSignAuthorities(username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Sign Role Authority list fetched", list)
        );
    }

    @PostMapping(value = "/add-sign-authority")
    public ResponseEntity<ApiResponse> addProgramData(@RequestBody SignRoleAuthorityDTO dto, @RequestHeader String username) {
        SignRoleAuthorityDTO data = masterService.addSignRoleAuthority(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Sign Role Authority added successfully", data)
        );
    }

    @PutMapping(value = "/update-sign-authority")
    public ResponseEntity<ApiResponse> updateProgramData(@RequestBody SignRoleAuthorityDTO dto, @RequestHeader String username) {
        Optional<SignRoleAuthorityDTO> data = masterService.updateSignRoleAuthority(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Sign Role Authority updated successfully", data)
        );
    }

    @GetMapping(value = "/project")
    public ResponseEntity<ApiResponse> getProjectMasterList(@RequestHeader String username) {
        List<ProjectMasterDTO> list = masterService.getProjectMasterList(username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Project master list fetched", list)
        );
    }

    @GetMapping(value = "/project-byId")
    public ResponseEntity<ApiResponse> getProjectListByEmpId(
            @RequestHeader String username,
            @RequestParam Long empId) {

        List<ProjectEmployeeDto> list = masterService.getProjectListByEmpId(empId, username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Project list fetched", list)
        );

    }

    @GetMapping(value = "/project-role-master")
    public ResponseEntity<ApiResponse> getProjectRoleMasterList(@RequestHeader String username, @RequestHeader(value = "Authorization") String token) {
        List<RoleMaster> list = masterService.getRoleMasterListed(username, token);
        return ResponseEntity.ok(
                new ApiResponse(true, "Role master list fetched", list)
        );
    }

    @PostMapping(value = "/add-project-role")
    public ResponseEntity<ApiResponse> addProjectRoleIds(@RequestHeader String username, @RequestBody ProjectAssignEmpDto dto,
                                                         @RequestHeader(value = "Authorization") String token) {
        ResponseEntity<String> response = masterService.addProjectsRolesIds(username, dto, token);

        if (response.getStatusCode().is2xxSuccessful() && "200".equals(response.getBody())) {
            return ResponseEntity.ok(
                    new ApiResponse(true, "Project roles assigned successfully", null)
            );
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Failed to assign project roles", null));
        }
    }

    @GetMapping("/user-login-app-access")
    public ResponseEntity<Boolean> checkUserLoginAccess(@RequestHeader(value = "username", required = false) String username, @RequestHeader(value = "Authorization", required = false) String token, @RequestParam ProjectCode projectCode) {

        try {
            return masterClient.checkUserLoginAccess(token, username, projectCode);
        } catch (Exception e) {
            //
            e.printStackTrace();
            return ResponseEntity.ok(false);
        }
    }

    @GetMapping("/get-react-app-urls")
    public ResponseEntity<List<ReactAppUrlDTO>> getAllReactAppUrls() {

        try {
            List<ReactAppUrlDTO> response = masterClient.getReactAppUrls(xApiKey);

            if (response == null || response.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
