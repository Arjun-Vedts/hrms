package com.vts.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YearlyRequisitionSummary {
    private String financialYear;
    private Long requisitions;
    private Long attended;
    private Long notAttended;
}
