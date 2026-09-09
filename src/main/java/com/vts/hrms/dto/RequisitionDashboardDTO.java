package com.vts.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequisitionDashboardDTO {

    private Long organisers;
    private Long courses;
    private Long requisitions;
    private Long attended;
    private List<CountTypeResponse> attendedByCadre;
    private List<CountTypeResponse> courseTypeCounts;
    private List<CountTypeResponse> courseParticipants;

}
