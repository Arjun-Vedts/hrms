package com.vts.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackDTO {

    private Long feedbackId;
    private Long requisitionId;
    private String requisitionNumber;
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

    private String courseName;
    private String organizer;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer programDuration;
    private String participantName;
    private String designation;
    private String divisionName;

    private String certificate;
    private String invoice;
    private MultipartFile certificateFile;
    private MultipartFile invoiceFile;

    private String isAccepted;
    private Long acceptedBy;
    private String acceptedByName;
    private LocalDateTime acceptedDate;
    private String requestThrough;

    private String isGroup;

    public FeedbackDTO(Long requisitionId, String requisitionNumber) {
        this.requisitionId = requisitionId;
        this.requisitionNumber = requisitionNumber;
    }
}
