package com.vts.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginEmployeeDto {

    private Long empId;
    private String empNo;
    private String employeeType;
    private String title;
    private String salutation;
    private String empName;
    private String empDesigName;
    private String empStatus;
    private String roleName;
    private String hindiRoleName;
    private Long divisionId;
    private Long loginId;
    private Long roleId;
    private String isGroup;
    private String hindiTitle;
    private String hindiSalutation;
    private String hindiEmpName;
    private String hindiEmpDesigName;

    public LoginEmployeeDto(Long empId, String roleName, Long loginId, Long roleId,String hindiRoleName) {
        this.empId = empId;
        this.roleName = roleName;
        this.loginId = loginId;
        this.roleId = roleId;
        this.hindiRoleName = hindiRoleName;
    }

}

