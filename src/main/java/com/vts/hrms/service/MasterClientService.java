package com.vts.hrms.service;

import com.vts.hrms.auth.AuthenticationRequest;
import com.vts.hrms.dto.*;
import com.vts.hrms.util.ErrorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "masterClient", url = "${feign_client_uri}")
public interface MasterClientService {


    @GetMapping("/getEmpDesigMaster")
    List<DesignationDTO> getEmpDesigMaster(@RequestHeader("X-API-KEY") String apiKey);

    @GetMapping("/getDivisionMaster")
    List<DivisionDTO> getDivisionMaster(@RequestHeader("X-API-KEY") String apiKey);

    @GetMapping("/getEmployee")
    List<EmployeeDTO> getEmployeeList(@RequestHeader("X-API-KEY") String apiKey);

    @GetMapping("/getEmployeeMaster")
    List<EmployeeDTO> getEmployeeMasterList(@RequestHeader("X-API-KEY") String apiKey);

    @GetMapping("/getEmployee")
    List<EmployeeDTO> getEmployee(@RequestHeader("X-API-KEY") String apiKey , @RequestParam("empId") long empId);

    @RequestMapping(value = "/getAuthenticate", method = RequestMethod.POST)
    ResponseEntity<ErrorResponse> getAuthenticate(@RequestBody AuthenticationRequest authenticationRequest);

    @GetMapping("/getProjectMaster")
    List<ProjectMasterDTO> getProjectMasterList(@RequestHeader("X-API-KEY") String apiKey);

    @GetMapping("/getProjectListByEmpId")
    List<ProjectEmployeeDto> getProjectListByEmpId(@RequestHeader("X-API-KEY") String apiKey, @RequestParam("empId") Long empId);

    @GetMapping(value = "/roleMaster")
    List<RoleMaster> getRoleMasterList(@RequestHeader(value = "Authorization") String token);

    @PostMapping(value = "/addProjectRoleId")
    ResponseEntity<String> addProjectRoleIds(@RequestBody ProjectAssignEmpDto dto, @RequestHeader("Authorization") String token, @RequestHeader("username") String username);

    @GetMapping(value = "/user-login-app-access" )
    ResponseEntity<Boolean> checkUserLoginAccess(@RequestHeader("Authorization") String token, @RequestHeader("username") String username, @RequestParam ProjectCode projectCode);

    @GetMapping( value = "/get-app-urls", consumes = MediaType.APPLICATION_JSON_VALUE )
    List<ReactAppUrlDTO> getReactAppUrls(@RequestHeader("X-API-KEY") String apiKey);

    @PutMapping( value = "/change-password" )
    Integer changePassword(@RequestHeader("Authorization") String token, @RequestHeader("username") String username, @RequestBody ChangePasswordDTO changePasswordDTO);

    @GetMapping("/getDivisionGroupList")
    List<DivisionGroupDTO> getDivisionGroupMasterList(@RequestHeader("X-API-KEY") String apiKey);

    @GetMapping("/getLabMaster")
    List<LabMasterDTO> getLabMaster(@RequestHeader("X-API-KEY") String apiKey);

}
