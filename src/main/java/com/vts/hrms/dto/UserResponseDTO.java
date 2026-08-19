package com.vts.hrms.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private Long loginId;
    private Long empId;
    private Long divisionId;

    private String username;
    private String employeeName;
    private String designationName;
    private String divisionName;

    private List<Long> roleIds;
    private List<String> roleNames;
    private List<String> hindiRoleNames;
}