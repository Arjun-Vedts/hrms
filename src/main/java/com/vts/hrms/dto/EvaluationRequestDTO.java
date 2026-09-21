package com.vts.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvaluationRequestDTO implements Serializable {

    private Long initiator;
    private Long preparedBy;
    private String empName;
    private String designation;
    private String title;
    private String preparedByEmpName;
    private List<EvaluationDTO> evaluation;
    private EvaluationDTO evaluationData;
}
