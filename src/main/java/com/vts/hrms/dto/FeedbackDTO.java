package com.vts.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackDTO {

    private Long feedbackId;
    private Long requisitionId;
    private LocalDate feedbackDate;
    private Long participantId;
    private String facultyName;
    private String facultyAddress;
    private String remark;
    private String course;
    private String coverage;
    private String duration;
    private String faculty;
    private String participant;
    private String courseVenue;
    private String quality;
    private String seminarVenue;
    private Integer isActive;

    private String programName;
    private String organizer;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer programDuration;
    private String participantName;
    private String divisionName;

}
