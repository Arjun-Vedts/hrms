package com.vts.hrms.controller;

import com.vts.hrms.dto.*;
import com.vts.hrms.service.DashboardService;
import com.vts.hrms.service.TrainingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final TrainingService trainingService;

    public DashboardController(DashboardService dashboardService, TrainingService trainingService) {
        this.dashboardService = dashboardService;
        this.trainingService = trainingService;
    }

    @GetMapping("/course-count")
    public ResponseEntity<List<CourseDashboardDTO>> getOrganizerCourseDashboard(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        List<CourseDashboardDTO> response =
                dashboardService.getOrganizerCourseDashboard(startDate, endDate);

        return ResponseEntity.ok(response);
    }


    @GetMapping(value = "/requisition-list")
    public ResponseEntity<List<RequisitionDTO>> getRequisitionList(@RequestParam Long empId, @RequestParam String roleName,
                                                                   @RequestHeader(value = "username", required = false) String username) {
        List<RequisitionDTO> list = trainingService.getRequisitionList(empId, roleName,null,null, null,username, "N");

        return ResponseEntity.ok(list);
    }


    @GetMapping("/requisition")
    public ResponseEntity<RequisitionDashboardDTO> getDashboardData(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        RequisitionDashboardDTO response = dashboardService.getDashboardData(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user-requisition")
    public ResponseEntity<RequisitionDashboardDTO> getUserDashboardData(
            @RequestParam Long empId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        RequisitionDashboardDTO response = dashboardService.getUserDashboardData(empId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user-evaluation")
    public ResponseEntity<Map<String,Long>> getUserEvaluationData(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        Map<String,Long> response = dashboardService.getUserEvaluationData(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user-req-pending")
    public ResponseEntity<List<FeedbackDTO>> getUserRequisitionPending(
            @RequestParam Long empId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        List<FeedbackDTO> response = dashboardService.getUserRequisitionPending(empId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/yearly-trend")
    public ResponseEntity<List<YearlyRequisitionSummary>> getUserYearlyTrend(
            @RequestParam Long empId,
            @RequestParam(defaultValue = "7") int years) {
        return ResponseEntity.ok(dashboardService.getUserYearlyTrend(empId, years));
    }

}
