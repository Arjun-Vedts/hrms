package com.vts.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CourseDTO implements Serializable {

    private Long courseId;
    private String courseCode;

    @NotBlank(message = "Course Name is required")
    private String courseName;

    private String courseLevel;
    private Long courseTypeId;
    private String courseType;

    @NotNull(message = "Eligibility is required")
    private Long eligibilityId;


    private String eligibilityName;

    @NotNull(message = "Organizer is required")
    private Long organizerId;

    private String organizer;

    private BigDecimal offlineRegistrationFee;

    private BigDecimal onlineRegistrationFee;

    @NotBlank(message = "Venue is required")
    private String venue;

    private Long noOfNomination;

    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    private LocalDate toDate;

    private String brochureName;

    private MultipartFile multipartFileBrochure;

}
