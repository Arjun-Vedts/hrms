package com.vts.hrms.controller;

import com.vts.hrms.dto.CourseDashboardDTO;
import com.vts.hrms.dto.RequisitionDTO;
import com.vts.hrms.dto.RequisitionDashboardDTO;
import com.vts.hrms.dto.YearlyRequisitionSummary;
import com.vts.hrms.service.DashboardService;
import com.vts.hrms.service.TrainingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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

    @GetMapping("/user/yearly-trend")
    public ResponseEntity<List<YearlyRequisitionSummary>> getUserYearlyTrend(
            @RequestParam Long empId,
            @RequestParam(defaultValue = "7") int years) {
        return ResponseEntity.ok(dashboardService.getUserYearlyTrend(empId, years));
    }

}
