package com.vts.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequisitionDTO implements Serializable {

    private Long requisitionId;

    @NotNull(message = "Course is required")
    private Long courseId;

    private String requisitionNumber;

    @NotNull(message = "Initiating Officer is required")
    private Long initiatingOfficer;

    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    private LocalDate toDate;

    @NotNull(message = "Duration is required")
    private Integer duration;

    @NotBlank(message = "Mode of payment is required")
    private String modeOfPayment;

    @NotBlank(message = "Course submission is required")
    private String isSubmitted;

    private String isPaperPresent;

    private String courseLevel;
    private String courseType;
    private BigDecimal registrationFee;
    private String reference;
    private String venue;
    private String necessity;
    private BigDecimal offlineRegistrationFee;
    private BigDecimal onlineRegistrationFee;

    private String reason;
    private String status;
    private String statusName;
    private String statusColor;
    private String fileEcs;
    private String fileCheque;
    private String filePan;
    private String fileBrochure;
    private String fileCommitteeApproval;
    private String fileAcceptanceLetter;
    private String filePaper;

    private String courseName;
    private Long organizerId;
    private String organizer;
    private String organizerContactName;
    private String organizerPhoneNo;
    private String organizerFaxNo;
    private String organizerEmail;

    private Long actionBy;
    private Long actionTo;
    private Long verifiedBy;
    private Long approvedBy;
    private String initiatingOfficerName;
    private String verifiedOfficerName;
    private String approvedOfficerName;
    private String empNo;
    private String empDesigName;
    private String desigCadre;
    private String empDivCode;
    private String email;
    private String mobileNo;

    private String remarks;
    private String isGroup;

    private Long journalId;
    private String titleOfPaper;

    private String isMandatory;
    private String isConfirmed;
    private String confirmRemarks;
    private String confirmFileName;
    private String isAttend;
    private String attendRemarks;

    private String forwardByName;
    private LocalDateTime forwardDate;
    private LocalDateTime verifiedDate;
    private LocalDateTime approvedDate;

    private MultipartFile multipartFileEcs;
    private MultipartFile multipartFileCheque;
    private MultipartFile multipartFilePan;
    private MultipartFile multipartFileBrochure;
    private MultipartFile multipartCommitteeApproval;
    private MultipartFile multipartAcceptanceLetter;
    private MultipartFile multipartPaper;
}
