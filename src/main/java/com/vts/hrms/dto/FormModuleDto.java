package com.vts.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormModuleDto {

    private Long formModuleId ;
    private String formModuleName ;
    private String hindiFormModuleName;
    private String moduleUrl ;
    private String moduleIcon ;
    private int serialNo ;
    private int isActive ;

}

