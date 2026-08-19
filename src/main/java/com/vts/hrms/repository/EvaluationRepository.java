package com.vts.hrms.repository;

import com.vts.hrms.dto.EvaluationDTO;
import com.vts.hrms.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {


    @Query("""
                SELECT new com.vts.hrms.dto.EvaluationDTO(
                    e.evaluationId,
                    e.requisitionId,
                    e.traineeId,
                    p.courseId,
                    e.impact,
                    p.courseName,
                    r.fromDate,
                    r.toDate
                )
                FROM Evaluation e
                LEFT JOIN Requisition r ON r.requisitionId = e.requisitionId
                AND r.status IN ("CO", "FA") AND r.isAttend = "Y"
                LEFT JOIN Course p ON p.courseId = r.courseId
                WHERE e.isActive = 1
                AND r.fromDate >= :fromDate AND r.toDate <= :toDate
                ORDER BY r.fromDate DESC
            """)
    List<EvaluationDTO> findEvaluationData(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("""
                SELECT new com.vts.hrms.dto.EvaluationDTO(
                    e.evaluationId,
                    e.requisitionId,
                    e.traineeId,
                    p.courseId,
                    e.impact,
                    p.courseName,
                    r.fromDate,
                    r.toDate
                )
                FROM Evaluation e
                LEFT JOIN Requisition r ON r.requisitionId = e.requisitionId
                LEFT JOIN Course p ON p.courseId = r.courseId
                WHERE e.isActive = 1 AND e.traineeId = :id
            """)
    List<EvaluationDTO> findByEmployee(@Param("id") Long id);
}
