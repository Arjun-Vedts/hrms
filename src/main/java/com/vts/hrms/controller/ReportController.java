package com.vts.hrms.controller;

import com.vts.hrms.dto.*;
import com.vts.hrms.service.ReportService;
import com.vts.hrms.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger LOG = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportsService;

    @GetMapping("/nominal-roll")
    public ResponseEntity<ApiResponse> getTotalNominalData(@RequestHeader("Authorization") String token) {
        LOG.info(" REST request to fetch nominal roll list");
        List<EmployeeDTO> list = reportsService.getNominalRollList(token);
        return ResponseEntity.ok(
                new ApiResponse(true, "Nominal roll list fetched successfully", list)
        );
    }

    @GetMapping("/course-training")
    public ResponseEntity<ApiResponse> getCourseTrainingData(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        LOG.info(" Request to fetch course training list for period {} - {} ", fromDate, toDate);
        List<RequisitionDTO> list = reportsService.getCourseTrainingList(fromDate, toDate, "course");
        list = list.stream().filter(data -> "Y".equalsIgnoreCase(data.getIsAttend())).toList();
        return ResponseEntity.ok(
                new ApiResponse(true, "Course training list fetched successfully", list)
        );
    }

    @GetMapping("/seminar-training")
    public ResponseEntity<ApiResponse> getSeminarTrainingData(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        LOG.info(" Request to fetch seminar training list for period {} - {} ", fromDate, toDate);
        List<RequisitionDTO> list = reportsService.getCourseTrainingList(fromDate, toDate, "seminar");
        list = list.stream().filter(data -> "Y".equalsIgnoreCase(data.getIsAttend())).toList();
        return ResponseEntity.ok(
                new ApiResponse(true, "Seminar training list fetched successfully", list)
        );
    }

    @GetMapping("/cep")
    public ResponseEntity<ApiResponse> getCEPData(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate, @RequestHeader String username) {
        LOG.info(" Request to fetch CEP list for period {} - {} ", fromDate, toDate);
        List<CepDTO> list = reportsService.getCEPData(fromDate, toDate, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "CEP list fetched successfully", list)
        );
    }

    @GetMapping("/sponsor-mtech")
    public ResponseEntity<ApiResponse> getMTechData(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        LOG.info(" Request to fetch Sponsorship M.Tech list for period {} - {} ", fromDate, toDate);
        List<SponsorshipDTO> list = reportsService.getMTechData(fromDate,toDate);
        return ResponseEntity.ok(
                new ApiResponse(true, "Sponsorship M.Tech list fetched successfully", list)
        );
    }

    @GetMapping("/sponsor-phd")
    public ResponseEntity<ApiResponse> getPhdData(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        LOG.info(" Request to fetch Sponsorship Phd list for period {} - {} ", fromDate, toDate);
        List<SponsorshipDTO> list = reportsService.getPhdData(fromDate,toDate);
        return ResponseEntity.ok(
                new ApiResponse(true, "Sponsorship Phd list fetched successfully", list)
        );
    }

    @GetMapping("/hr-distribution")
    public ResponseEntity<ApiResponse> getHrDistributionData(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        LOG.info(" Request to fetch HR distribution list for period {} - {} ", fromDate, toDate);
        List<DistributionDTO> list = reportsService.getHrDistributionData(fromDate,toDate);
        return ResponseEntity.ok(
                new ApiResponse(true, "HR distribution list fetched successfully", list)
        );
    }

    @GetMapping("/annual-training")
    public ResponseEntity<ApiResponse> getAnnualTrainingReport(@RequestParam LocalDate fromDate,
                                                               @RequestParam LocalDate toDate,
                                                               @RequestHeader("Authorization") String token) {
        LOG.info(" Request to fetch annual training report list for period {} - {} ", fromDate, toDate);
        List<AnnualTrainingReportDTO> list = reportsService.getAnnualTrainingReport(fromDate,toDate, token);
        return ResponseEntity.ok(
                new ApiResponse(true, "Annual training report list fetched successfully", list)
        );
    }

    @GetMapping("/budget-expenditure")
    public ResponseEntity<ApiResponse> getBudgetExpenditureReport(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate, @RequestHeader String username) {
        LOG.info(" Request to fetch budget expenditure report list for period {} - {} ", fromDate, toDate);
        List<BudgetExpenditureDTO> list = reportsService.getBudgetExpenditureReport(fromDate, toDate, username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Budget expenditure report list fetched successfully", list)
        );
    }

    @GetMapping("/gender-budgeting")
    public ResponseEntity<ApiResponse> getGenderBudgetReport(@RequestHeader("Authorization") String token) {
        LOG.info(" Request to fetch gender budgeting report list");
        List<GenderBudgetDTO> data = reportsService.getGenderBudgetReport(token);
        return ResponseEntity.ok(
                new ApiResponse(true, "Gender budget report list fetched successfully", data)
        );
    }

    @GetMapping("/training-scst")
    public ResponseEntity<ApiResponse> getTrainingSCSTReport(@RequestHeader("Authorization") String token,@RequestHeader String username) {
        LOG.info(" Request to fetch training scst report list");
        List<TrainingSCSTDTO> data = reportsService.getTrainingSCSTReport(token,username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Training SC/ST report list fetched successfully", data)
        );
    }

    @GetMapping("/research-paper-int")
    public ResponseEntity<ApiResponse> getResearchPaperIntReport(@RequestHeader String username) {
        LOG.info(" Request to fetch research paper international report list");
        List<JournalDTO> data = reportsService.getResearchPaperIntReport(username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Research paper int. report list fetched successfully", data)
        );
    }

    @GetMapping("/research-paper")
    public ResponseEntity<ApiResponse> getResearchPaperDetailReport(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate, @RequestHeader String username) {
        LOG.info(" Request to fetch research paper detail report list for period {} - {}", fromDate, toDate);
        List<RequisitionDTO> data = reportsService.getResearchPaperDetailReport(fromDate,toDate,username);
        return ResponseEntity.ok(
                new ApiResponse(true, "Research paper detail report list fetched successfully", data)
        );
    }

    @GetMapping("/course")
    public ResponseEntity<List<RequisitionDTO>> getAllCourseTrainingData() {
        LOG.info(" Request to fetch all course training list ");
        List<RequisitionDTO> list = reportsService.getAllCourseTrainingList();
        return ResponseEntity.ok(list);
    }

}
