package com.vts.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvaluationDTO implements Serializable {

    private Long evaluationId;
    private Long requisitionId;
    private Long traineeId;
    private Long preparedBy;
    private Long courseId;
    private String impact;
    private String courseName;
    private LocalDate fromDate;
    private LocalDate toDate;

}
