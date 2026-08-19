package com.vts.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EmployeeDTO implements Serializable {

    private Long empId;
    private String title;
    private String hindiTitle;
    private String salutation;
    private String hindiSalutation;
    private String empNo;
    private String employeeType;
    private String empName;
    private String hindiEmpName;
    private Long designationId;
    private Long divisionId;
    private String empStatus;

    private Long srNo;
    private String empDesigName;
    private String hindiDesignation;
    private String empDesigCode;
    private String desigCadre;
    private String empDivCode;
    private String roleName;
    private String hindiRoleName;
    private String maritalStatus;
    private String gender;
    private String email;
    private String mobileNo;
    private String username;
    private String labCode;
    private Long desigId;
    private String extNo;
    private String dronaEmail;
    private String internalEmail;
    private String internetEmail;
    private Long superiorOfficer;
    private int isActive;
    private String photo;

    private String desigGroup;
    private String desigSubCadre;
    private String officerType;

    private String currentAddress;
    private String permanentAddress;
    private String hometownAddress;
    private String bankFullDetails;
    private String passportFullDetails;

    private EmployeeDetailsDTO employeeDetails;
    private LabDetailsDTO labDetails;

    private String isGroup;

}
