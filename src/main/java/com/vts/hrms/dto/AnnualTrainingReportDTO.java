package com.vts.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnualTrainingReportDTO implements Serializable {

    private Long courseTypeId;
    private String course;
    private Long withinLab = 0L;
    private Long otherLabs = 0L;

    // Course.courseLevel = National
    private Long outsideWithinIndia = 0L;

    // Course.courseLevel = International
    private Long outsideForeignAgency = 0L;

    private Long subTotal = 0L;

    private Long drdsTni = 0L;
    private Long drdsRacBoard = 0L;
    private Long drdsOpen = 0L;
    private Long drdsTotal = 0L;
    private Long drdsMale = 0L;
    private Long drdsFemale = 0L;

    private Long drtcTni = 0L;
    private Long drtcCeptamBoard = 0L;
    private Long drtcOpen = 0L;
    private Long drtcTotal = 0L;
    private Long drtcMale = 0L;
    private Long drtcFemale = 0L;

    private Long adminAlliedTni = 0L;
    private Long adminAlliedOpen = 0L;
    private Long adminAlliedTotal = 0L;
    private Long adminAlliedMale = 0L;
    private Long adminAlliedFemale = 0L;

    private Long othersTotal = 0L;
    private Long othersMale = 0L;
    private Long othersFemale = 0L;
}

