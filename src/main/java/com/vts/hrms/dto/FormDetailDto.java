package com.vts.hrms.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@Builder
@ToString
public class FormDetailDto {

    private Long formDetailId ;
    private Long formModuleId ;
    private String formName;
    private String formUrl;
    private String formDispName;
    private String hindiFormDispName;
    private int formSerialNo ;
    private String formColor;
    private int isActive ;
    private String modifiedBy;
    private LocalDateTime modifiedDate;

}