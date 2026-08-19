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

    private Long FormModuleId ;
    private String FormModuleName ;
    private String hindiFormModuleName;
    private String ModuleUrl ;
    private String ModuleIcon ;
    private int SerialNo ;
    private int IsActive ;

}

