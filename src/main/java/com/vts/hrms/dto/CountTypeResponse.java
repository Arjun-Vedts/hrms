package com.vts.hrms.dto;

import lombok.*;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CountTypeResponse {

    private String type;
    private Long count;
    private String courseName;
    private Map<String, Long> cadreCounts;
}
