package com.vts.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class DivisionDTO implements Serializable {

    private Long divisionId;
    private Long groupId;

    @NotBlank(message = "Division code is required")
    private String divisionCode;

    @NotBlank(message = "Division name is required")
    private String divisionName;

    private String divisionShortName;

    @NotNull(message = "Division Head is required")
    private Long divisionHeadId;

    private String divHeadName;
    private String isGroup;
    private int isActive;

}
