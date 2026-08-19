package com.vts.hrms.controller;

import com.vts.hrms.dto.*;
import com.vts.hrms.exception.NotFoundException;
import com.vts.hrms.service.TrainingService;
import com.vts.hrms.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/training")
public class TrainingController {

    @Value("${appStorage}")
    private String appStorage;

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @GetMapping(value = "/organizer")
    public ResponseEntity<ApiResponse> getAllOrganizers() {
        List<OrganizerDTO> list = trainingService.getAllAgencies();

        return ResponseEntity.ok(
                new ApiResponse(true, "Organizer list fetched", list)
        );
    }

    @GetMapping(value = "/calender")
    public ResponseEntity<ApiResponse> getCalenderList(@RequestParam String year, @RequestHeader String username) {
        List<CalendarDTO> list = trainingService.getCalenderList(year, username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Calender list fetched", list)
        );
    }

    @PostMapping(value = "/add-calendar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> addCalenderData(@Valid @ModelAttribute CalendarDTO dto, @RequestHeader String username) throws IOException {
        CalendarDTO data = trainingService.addCalenderData(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Calendar data added successfully", data)
        );
    }


    @PutMapping(value = "/update-calendar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> editCalendarData(@Valid @ModelAttribute CalendarDTO dto, @RequestHeader String username) throws IOException {
        Optional<CalendarDTO> data = trainingService.updateCalendarData(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Calendar data updated successfully", data)
        );
    }

    @GetMapping(value = "/calendar-file/{id}/{fileType}")
    public ResponseEntity<Resource> downloadCalendarFile(@PathVariable Long id, @PathVariable String fileType, @RequestHeader String username) {
        try {

            if (fileType.isEmpty()) {
                throw new NotFoundException("Filetype not found while download calendar files.");
            }
            if (id == null) {
                throw new NotFoundException("Calendar Id can not be null");
            }

            CalendarDTO dto = trainingService.getCalendarById(id, username)
                    .orElseThrow(() -> new NotFoundException("Calendar data not found"));

            OrganizerDTO organizerDTO = trainingService.getOrganizerById(dto.getOrganizerId())
                    .orElseThrow(() -> new NotFoundException("Organizer data not found"));

            Path filePath = Paths.get(appStorage, "Calendar", dto.getYear(), organizerDTO.getOrganizer().trim());

            if ("CF".equalsIgnoreCase(fileType)) {
                filePath = filePath.resolve(dto.getCalendarFileName());
            } else {
                filePath = filePath.resolve(dto.getCoveringLetter());
            }

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            String fileName = dto.getCalendarFileName();
            String contentType = Files.probeContentType(filePath);

            // Fallback content type
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/add-course")
    public ResponseEntity<ApiResponse> addCourseData(@Valid @RequestBody CourseDTO dto, @RequestHeader String username) throws IOException {
        CourseDTO data = trainingService.addCourseData(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Course data added successfully", data)
        );
    }

    @PutMapping(value = "/edit-course")
    public ResponseEntity<ApiResponse> editCourseData(@Valid @RequestBody CourseDTO dto, @RequestHeader String username) throws IOException {
        Optional<CourseDTO> data = trainingService.editCourseData(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Course data edited successfully", data)
        );
    }

    @PostMapping(value = "/add-organizer")
    public ResponseEntity<ApiResponse> addOrganizer(@Valid @RequestBody OrganizerDTO dto, @RequestHeader String username) throws IOException {
        OrganizerDTO data = trainingService.addOrganizer(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Organizer data added successfully", data)
        );
    }

    @PutMapping(value = "/edit-organizer")
    public ResponseEntity<ApiResponse> editOrganizer(@Valid @RequestBody OrganizerDTO dto, @RequestHeader String username) throws IOException {
        Optional<OrganizerDTO> data = trainingService.editOrganizer(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Organizer data edited successfully", data)
        );
    }

    @GetMapping(value = "/course")
    public ResponseEntity<ApiResponse> getCourseList(@RequestParam Long orgId, @RequestHeader String username) {
        List<CourseDTO> list = trainingService.getCourseList(orgId, username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Course list fetched", list)
        );
    }

    @GetMapping(value = "/course-type")
    public ResponseEntity<ApiResponse> getCourseTypeList(@RequestHeader String username) {
        List<CourseTypeDTO> list = trainingService.getCourseTypeList(username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Course type list fetched", list)
        );
    }

    @PostMapping(value = "/add-requisition", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> addRequisitionData(@Valid @ModelAttribute RequisitionDTO dto, @RequestHeader String username) throws IOException {
        RequisitionDTO data = trainingService.addRequisitionData(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition data added successfully", data)
        );
    }

    @GetMapping(value = "/requisition")
    public ResponseEntity<ApiResponse> getRequisitionList(@RequestParam Long empId, @RequestParam String roleName, @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate, @RequestParam(required = false) Long selectedEmployeeId, @RequestHeader String username) {
        List<RequisitionDTO> list = trainingService.getRequisitionList(empId, roleName, fromDate, toDate, selectedEmployeeId, username, "N");

        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition list fetched", list)
        );
    }

    @GetMapping(value = "/requisition/{id}")
    public ResponseEntity<ApiResponse> getRequisitionById(@PathVariable Long id, @RequestHeader String username) {
        RequisitionDTO data = trainingService.getRequisitionById(id, username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition data fetched", data)
        );
    }

    @GetMapping(value = "/req-file/{id}/{file}")
    public ResponseEntity<Resource> downloadRequisitionFile(@PathVariable Long id, @PathVariable String file, @RequestHeader String username) {
        try {

            RequisitionDTO requisitionDTO = trainingService.getRequisitionById(id, username);
            Path filePath = Paths.get(appStorage, "Requisition", requisitionDTO.getRequisitionNumber().replace("/", "_"), file);

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);

            // Fallback content type
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/update-requisition", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> updateRequisitionData(@Valid @ModelAttribute RequisitionDTO dto, @RequestHeader String username) throws IOException {
        Optional<RequisitionDTO> data = trainingService.updateRequisitionData(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition data updated successfully", data)
        );
    }


    @PostMapping(value = "/requisition-feedback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> requisitionFeedback(@ModelAttribute FeedbackDTO dto, @RequestHeader String username) throws IOException {
        FeedbackDTO data = trainingService.requisitionFeedback(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Feedback submitted successfully", data)
        );
    }

    @GetMapping(value = "/feedback-list")
    public ResponseEntity<ApiResponse> getFeedbackList(@RequestParam Long empId, @RequestParam String roleName, @RequestHeader String username) {
        List<FeedbackDTO> list = trainingService.getFeedbackList(empId, roleName, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Feedback list fetched successfully", list)
        );
    }

    @GetMapping(value = "/feedback-list-date-range")
    public ResponseEntity<ApiResponse> getFeedbackListByDateRange(@RequestParam Long empId, @RequestParam String roleName,
                                                                  @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate,
                                                                  @RequestHeader String username) {
        List<FeedbackDTO> list = trainingService.getFeedbackListByDateRange(empId, roleName, fromDate, toDate);
        return ResponseEntity.ok(
                new ApiResponse(true, "Feedback list fetched successfully", list)
        );
    }

    @GetMapping(value = "/feedback-data/{id}")
    public ResponseEntity<ApiResponse> getFeedbackById(@PathVariable Long id, @RequestHeader String username) {
        FeedbackDTO list = trainingService.getFeedbackById(id, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Feedback data fetched successfully", list)
        );
    }

    @PutMapping(value = "/update-feedback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> updateFeedback(@ModelAttribute FeedbackDTO dto, @RequestHeader String username) throws IOException {
        Optional<FeedbackDTO> data = trainingService.updateFeedback(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Feedback updated successfully", data)
        );
    }

    @PostMapping(value = "/accept-feedback")
    public ResponseEntity<ApiResponse> acceptFeedback(@RequestBody FeedbackDTO dto, @RequestHeader String username) throws IOException {
        FeedbackDTO data = trainingService.acceptFeedback(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Feedback accepted successfully", data)
        );
    }

    @GetMapping(value = "/feedback-print/{id}")
    public ResponseEntity<ApiResponse> getFeedbackPrint(@PathVariable Long id, @RequestHeader String username) {
        FeedbackDTO data = trainingService.getFeedbackPrint(id, username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Feedback print data fetched", data)
        );
    }

    @PostMapping(value = "/forward-req")
    public ResponseEntity<ApiResponse> forwardRequisition(@Valid @RequestBody RequisitionDTO dto, @RequestHeader String username) {
        RequisitionDTO data = trainingService.forwardRequisition(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition forwarded successfully", data)
        );
    }

    @PostMapping(value = "/revoke-req")
    public ResponseEntity<ApiResponse> revokeRequisition(@Valid @RequestBody RequisitionDTO dto, @RequestHeader String username) {
        RequisitionDTO data = trainingService.revokeRequisition(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition revoked successfully", data)
        );
    }

    @PostMapping(value = "/recommend-req")
    public ResponseEntity<ApiResponse> recommendRequisition(@Valid @RequestBody RequisitionDTO dto, @RequestHeader String username) {
        RequisitionDTO data = trainingService.recommendRequisition(dto, username);
        String message = data.getStatus().equalsIgnoreCase("AR") ? "Requisition recommended successfully" : "Requisition approved successfully";
        return ResponseEntity.ok(
                new ApiResponse(true, message, data)
        );
    }

    @PostMapping(value = "/return-req")
    public ResponseEntity<ApiResponse> returnRequisition(@Valid @RequestBody RequisitionDTO dto, @RequestHeader String username) {
        RequisitionDTO data = trainingService.returnRequisition(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition returned successfully", data)
        );
    }

    @GetMapping(value = "/requisition-print/{id}")
    public ResponseEntity<ApiResponse> getRequisitionPrint(@PathVariable Long id, @RequestHeader String username) {
        RequisitionDTO data = trainingService.getRequisitionPrint(id, username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition print data fetched", data)
        );
    }

    @GetMapping(value = "/req-approval-list")
    public ResponseEntity<ApiResponse> getRequisitionApprovalList(@RequestParam Long empId, @RequestHeader String username) {
        List<RequisitionDTO> list = trainingService.getRequisitionApprovalList(empId, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition Approval list fetched successfully", list)
        );
    }

    @GetMapping(value = "/req-transaction/{reqId}")
    public ResponseEntity<ApiResponse> getRequisitionTransaction(@PathVariable Long reqId, @RequestHeader String username) {
        List<RequisitionTransactionDTO> list = trainingService.getRequisitionTransaction(reqId, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition transaction fetched successfully", list)
        );
    }

    @PostMapping(value = "/add-evaluation")
    public ResponseEntity<ApiResponse> addEvaluation(@RequestBody EvaluationRequestDTO dto, @RequestHeader String username) {
        EvaluationRequestDTO data = trainingService.addEvaluation(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Evaluation added successfully", data)
        );
    }

    @GetMapping(value = "/evaluation")
    public ResponseEntity<ApiResponse> getEvaluationList(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate, @RequestHeader String username) {
        List<EvaluationRequestDTO> list = trainingService.getEvaluationList(fromDate, toDate, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Evaluation list fetched", list)
        );
    }

    @GetMapping(value = "/evaluation-print/{id}")
    public ResponseEntity<ApiResponse> getEvaluationPrint(@PathVariable Long id, @RequestHeader String username) {
        EvaluationRequestDTO data = trainingService.getEvaluationPrint(id, username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Evaluation print data fetched", data)
        );
    }

    @GetMapping(value = "/eligibility")
    public ResponseEntity<ApiResponse> getEligibilityList(@RequestHeader String username) {
        List<EligibilityDTO> list = trainingService.getEligibilityList(username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Eligibility list fetched", list)
        );
    }


    @PostMapping(value = "/add-eligible")
    public ResponseEntity<ApiResponse> addEligibleData(@Valid @RequestBody EligibilityDTO dto, @RequestHeader String username) {
        EligibilityDTO data = trainingService.addEligibleData(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Eligibility data added successfully", data)
        );
    }

    @PutMapping(value = "/update-eligible")
    public ResponseEntity<ApiResponse> updateEligibleData(@Valid @RequestBody EligibilityDTO dto, @RequestHeader String username) {
        Optional<EligibilityDTO> data = trainingService.updateEligibleData(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Eligibility data updated successfully", data)
        );
    }

    @GetMapping(value = "/feedback-file/{feedId}/{type}")
    public ResponseEntity<Resource> downloadFeedbackFile(@PathVariable Long feedId, @PathVariable String type, @RequestHeader String username) {
        try {

            FeedbackDTO dto = trainingService.getFeedbackById(feedId, username);
            RequisitionDTO requisitionDTO = trainingService.getRequisitionById(dto.getRequisitionId(), username);
            Path fullpath = Paths.get(appStorage, "Requisition",
                    requisitionDTO.getRequisitionNumber().replace("/", "_"), "Feedback");
            Path filePath = null;

            if (type.equalsIgnoreCase("certificate")) {
                if (dto.getCertificate() != null && !dto.getCertificate().isEmpty()) {
                    filePath = fullpath.resolve(dto.getCertificate());
                }
            }
            if (type.equalsIgnoreCase("invoice")) {
                if (dto.getInvoice() != null && !dto.getInvoice().isEmpty()) {
                    filePath = fullpath.resolve(dto.getInvoice());
                }
            }

            assert filePath != null;
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);

            // Fallback content type
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + type + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/req-approved-list")
    public ResponseEntity<ApiResponse> getRequisitionApprovedList(@RequestParam Long empId, @RequestHeader String username) {
        List<RequisitionDTO> list = trainingService.getRequisitionApprovedList(empId, username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition approved list fetched", list)
        );
    }

    @PostMapping(value = "/forward-director")
    public ResponseEntity<ApiResponse> reqForwardToDirector(@RequestBody DirectorApproveDTO dto, @RequestHeader String username) {
        DirectorApproveDTO data = trainingService.reqForwardToDirector(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition forward to director successfully", data)
        );
    }

    @PostMapping(value = "/approve-req")
    public ResponseEntity<ApiResponse> approveRequisition(@RequestBody DirectorApproveDTO dto, @RequestHeader String username) {
        DirectorApproveDTO data = trainingService.approveRequisition(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition approved successfully", data)
        );
    }

    @PostMapping(value = "/recommend-dfa")
    public ResponseEntity<ApiResponse> recommendReqToDFA(@RequestBody DirectorApproveDTO dto, @RequestHeader String username) {
        DirectorApproveDTO data = trainingService.recommendReqToDFA(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition recommend to DFA successfully", data)
        );
    }

    @GetMapping(value = "/cep")
    public ResponseEntity<ApiResponse> getCepList(@RequestHeader String username) {
        List<CepDTO> list = trainingService.getAllCepData(username);

        return ResponseEntity.ok(
                new ApiResponse(true, "CEP list fetched", list)
        );
    }

    @GetMapping(value = "/cepById/{cepId}")
    public ResponseEntity<ApiResponse> getCepListBYID(@PathVariable Long cepId, @RequestHeader String username) {
        CepDTO list = trainingService.getCepID(cepId, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "CEP data fetched successfully", list)
        );
    }

    @PostMapping(value = "/add-cep", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> addCepData(
            @Valid @ModelAttribute CepDTO dto,
            @RequestHeader String username) throws IOException {
        CepDTO data = trainingService.addCepData(dto, username);

        return ResponseEntity.ok(
                new ApiResponse(true, "CEP data added successfully", data)
        );
    }

    @PutMapping(value = "/edit-cep", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> editCepData(@Valid @ModelAttribute CepDTO dto, @RequestHeader String Username) throws IOException {
        Optional<CepDTO> list = trainingService.editCEPData(dto, Username);
        return ResponseEntity.ok(
                new ApiResponse(true, "CEP data Updated  successfully", list)
        );
    }

    @GetMapping(value = "/cep-file/{attachmentId}")
    public ResponseEntity<Resource> downloadRequisitionFile(@PathVariable Long attachmentId, @RequestHeader String username) {
        try {

            CepAttachmentsDTO attachment = trainingService.getCepAttachmentById(attachmentId, username);
            String folderName = "CEP_" + attachment.getCepId();
            Path filePath = Paths.get(appStorage, "CEP Attachments", folderName, attachment.getExistingFileName());

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);

            // Fallback content type
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getExistingFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping(value = "/distribution")
    public ResponseEntity<ApiResponse> getHrDistribution(@RequestHeader String username) {
        List<DistributionDTO> list = trainingService.getAllDistributionsData(username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Distribution list fetched", list)
        );
    }

    @GetMapping(value = "/distributionById/{id}")
    public ResponseEntity<ApiResponse> getDistributionByID(@PathVariable Long id, @RequestHeader String username) {
        DistributionDTO list = trainingService.getDistributionByID(id, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Distribution data fetched successfully", list)
        );
    }

    @PostMapping(value = "/add-distributions")
    public ResponseEntity<ApiResponse> addDistributionDATA(
            @Valid @RequestBody DistributionDTO dto,
            @RequestHeader String username) {
        DistributionDTO data = trainingService.addDistributionData(dto, username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Distributions data added successfully", data)
        );
    }

    @PutMapping(value = "/edit-distribution")
    public ResponseEntity<ApiResponse> editDistributionData(@Valid @RequestBody DistributionDTO dto, @RequestHeader String Username) {
        Optional<DistributionDTO> data = trainingService.editDistributionData(dto, Username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Distribution Updated successfully", data)
        );
    }

    @GetMapping(value = "/journal")
    public ResponseEntity<ApiResponse> getJournalList(@RequestParam Long empId, @RequestParam String roleName, @RequestHeader String username) {
        List<JournalDTO> list = trainingService.getJournalList(empId, roleName, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Journal list fetched successfully", list)
        );
    }

    @PostMapping(value = "/add-journal")
    public ResponseEntity<ApiResponse> addJournalData(@Valid @RequestBody JournalDTO dto, @RequestHeader String username) {
        JournalDTO data = trainingService.addJournalData(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Journal data added successfully", data)
        );
    }

    @PutMapping(value = "/edit-journal")
    public ResponseEntity<ApiResponse> editJournalData(@Valid @RequestBody JournalDTO dto, @RequestHeader String Username) {
        Optional<JournalDTO> list = trainingService.editJournalData(dto, Username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Journal data Updated successfully", list)
        );
    }

    @GetMapping(value = "/mandatory-training")
    public ResponseEntity<ApiResponse> getMandatoryTrainingList(@RequestParam Long empId, @RequestParam String roleName, @RequestHeader String username) {
        List<RequisitionDTO> list = trainingService.getRequisitionList(empId, roleName, null, null, null, username, "Y");

        return ResponseEntity.ok(
                new ApiResponse(true, "Requisition of mandatory training list fetched", list)
        );
    }

    @PostMapping(value = "/accept-training")
    public ResponseEntity<ApiResponse> acceptMandatoryTraining(@RequestBody RequisitionDTO dto, @RequestHeader String username) {
        RequisitionDTO data = trainingService.acceptMandatoryTraining(dto, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Mandatory training accepted successfully", data)
        );
    }

    @GetMapping("/mandatory-training/participant/{id}")
    public ResponseEntity<ApiResponse> getMandatoryTrainingByParticipantId(@PathVariable Long id, @RequestHeader String username) {
        List<RequisitionDTO> list = trainingService.getMandatoryTrainingByParticipantId(id, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Mandatory training data fetched successfully", list)
        );
    }

    @GetMapping(value = "/approved-list-byEmpId")
    public ResponseEntity<ApiResponse> getApprovedListByEmpId(@RequestParam Long empId, @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        List<RequisitionDTO> list = trainingService.getApprovedListByEmpId(empId, fromDate, toDate);
        return ResponseEntity.ok(
                new ApiResponse(true, "Role wise requisition approved list fetched successfully", list)
        );
    }

    @GetMapping("/approval-type")
    public ResponseEntity<String> getApprovalType(@RequestParam Long empId) {

        return ResponseEntity.ok(
                trainingService.getApprovalType(empId)
        );
    }

    @PostMapping(value = "/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> addReqConfirmation(@ModelAttribute RequisitionDTO dto,
                                                  @RequestHeader String username) throws IOException {
        RequisitionDTO data = trainingService.addConfirmation(dto, username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Confirmation details added successfully", data)
        );
    }

    @PostMapping(value = "/attend")
    public ResponseEntity<ApiResponse> addReqAttendance(@RequestBody RequisitionDTO dto,
                                                  @RequestHeader String username) {
        RequisitionDTO data = trainingService.addReqAttendance(dto, username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Attendance details added successfully", data)
        );
    }

    @GetMapping(value = "/lab-master")
    public ResponseEntity<ApiResponse> getLabMasterData(@RequestHeader String username) {
        LabMasterDTO data = trainingService.getLabMasterData(username);

        return ResponseEntity.ok(
                new ApiResponse(true, "Lab details fetched successfully", data)
        );
    }

}
