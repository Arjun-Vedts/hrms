package com.vts.hrms.repository;

import com.vts.hrms.dto.RequisitionDashboardDTO;
import com.vts.hrms.entity.Requisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface RequisitionRepository extends JpaRepository<Requisition, Long> {

    List<Requisition> findAllByIsActive(int isActive);

    List<Requisition> findAllByIsAttendAndIsActive(String attend, int isActive);

    List<Requisition> findAllByStatusInAndIsActive(List<String> statusCodes, int isActive);

    @Query("""
                SELECT DISTINCT r
                FROM Requisition r
                JOIN RequisitionTransaction t
                   ON r.requisitionId = t.requisitionId
                  WHERE t.actionTo IN :empIds
                  AND r.status=t.statusCode
                  AND t.statusCode IN :statusCodes
                  AND t.isActive = 1
                  AND r.isActive = 1
            """)
    List<Requisition> findApprovalList(
            @Param("empIds") List<Long> empIds,
            @Param("statusCodes") List<String> statusCodes
    );

    List<Requisition> findAllByIsActiveOrderByRequisitionIdDesc(int isActive);

    List<Requisition> findAllByInitiatingOfficerAndIsActiveOrderByRequisitionIdDesc(Long empId, int isActive);

    List<Requisition> findAllByInitiatingOfficerInAndIsActiveOrderByRequisitionIdDesc(List<Long> empIds, int isActive);

//    @Query("""
//            SELECT new com.vts.hrms.dto.RequisitionDashboardDTO(
//            c.organizerId,
//            o.organizer,
//            COUNT(r.requisitionId),
//            SUM(CASE WHEN r.status IN ('AA','REV','RR','RV') THEN 1 ELSE 0 END),
//            SUM(CASE WHEN r.status='AF' THEN 1 ELSE 0 END),
//            SUM(CASE WHEN r.status='AR' THEN 1 ELSE 0 END),
//            SUM(CASE WHEN r.status='AV' THEN 1 ELSE 0 END)
//            )
//            FROM Requisition r
//            JOIN Course c ON r.courseId = c.courseId
//            JOIN Organizer o ON c.organizerId = o.organizerId
//            GROUP BY c.organizerId,o.organizer
//            """)
//    List<RequisitionDashboardDTO> getOrganizerWiseRequisitionStats();
//
//    @Query("""
//            SELECT new com.vts.hrms.dto.RequisitionDashboardDTO(
//            c.organizerId,
//            o.organizer,
//            COUNT(r.requisitionId),
//            SUM(CASE WHEN r.status IN ('AA','REV','RR','RV') THEN 1 ELSE 0 END),
//            SUM(CASE WHEN r.status='AF' THEN 1 ELSE 0 END),
//            SUM(CASE WHEN r.status IN ('AR','AS','AV','CA') THEN 1 ELSE 0 END),
//            SUM(CASE WHEN r.status IN ('CO','DA') THEN 1 ELSE 0 END)
//            )
//            FROM Requisition r
//            JOIN Course c ON r.courseId = c.courseId
//            JOIN Organizer o ON c.organizerId = o.organizerId
//            WHERE r.isActive = 1
//            AND r.fromDate >= :startDate
//            AND r.toDate <= :endDate
//            GROUP BY c.organizerId,o.organizer
//            """)
//    List<RequisitionDashboardDTO> getRequisitionFilterDashboard(LocalDate startDate, LocalDate endDate);
//
//    @Query("""
//            SELECT new com.vts.hrms.dto.RequisitionDashboardDTO(
//            c.organizerId,
//            o.organizer,
//            COUNT(r.requisitionId) AS total,
//            SUM(CASE WHEN r.status IN ('AA','REV','RR','RV','RS') THEN 1 ELSE 0 END) AS pending,
//            SUM(CASE WHEN r.status='AF' THEN 1 ELSE 0 END) AS forwarded,
//            SUM(CASE WHEN r.status='AR' THEN 1 ELSE 0 END) AS recommended,
//            SUM(CASE WHEN r.status IN ('CO','DA') THEN 1 ELSE 0 END) AS approved
//            )
//            FROM Requisition r
//            JOIN Course c ON r.courseId = c.courseId
//            JOIN Organizer o ON c.organizerId = o.organizerId
//            WHERE r.isActive = 1
//            AND r.initiatingOfficer = :empId
//            AND r.fromDate >= :startDate
//            AND r.toDate <= :endDate
//            GROUP BY c.organizerId,o.organizer
//            """)
//    List<RequisitionDashboardDTO> getRequisitionFilterUserDashboard(Long empId, LocalDate startDate, LocalDate endDate);

    @Query("""
                SELECT r
                FROM Requisition r
                WHERE r.isActive = 1
                  AND r.fromDate >= :fromDate
                  AND r.toDate <= :toDate
                ORDER BY r.requisitionId DESC
            """)
    List<Requisition> getRequisitionDataByDateRange(@Param("fromDate") LocalDate fromDate,
                                                    @Param("toDate") LocalDate toDate);

    @Query("""
                SELECT r
                FROM Requisition r
                WHERE r.isActive = 1
                  AND r.initiatingOfficer = :empId
                  AND r.fromDate >= :fromDate
                  AND r.toDate <= :toDate
                ORDER BY r.requisitionId DESC
            """)
    List<Requisition> getUserRequisitionDataByDateRange(Long empId, LocalDate fromDate, LocalDate toDate);

    @Query("""
                SELECT r
                FROM Requisition r
                WHERE r.isActive = 1
                  AND r.journalId IS NOT NULL
                  AND r.journalId > 0
                  AND r.fromDate >= :fromDate
                  AND r.toDate <= :toDate
                ORDER BY r.requisitionId DESC
            """)
    List<Requisition> findActiveRequisitionsWithJournalId(@Param("fromDate") LocalDate fromDate,
                                                          @Param("toDate") LocalDate toDate);

    List<Requisition> findAllByRequisitionIdIn(Collection<Long> requisitionIds);

}
