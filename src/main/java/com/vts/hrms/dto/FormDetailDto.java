package com.vts.hrms.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@Builder
@ToString
public class FormDetailDto {

    private Long FormDetailId ;
    private Long FormModuleId ;
    private String FormName;
    private String FormUrl;
    private String FormDispName;
    private String hindiFormDispName;
    private int FormSerialNo ;
    private String FormColor;
    private int IsActive ;
    private String ModifiedBy;
    private LocalDateTime ModifiedDate;

}