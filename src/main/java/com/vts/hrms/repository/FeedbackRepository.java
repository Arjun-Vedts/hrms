package com.vts.hrms.repository;

import com.vts.hrms.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findAllByParticipantIdAndIsActiveOrderByFeedbackIdDesc(Long empId, int isActive);

    List<Feedback> findByIsActiveOrderByFeedbackIdDesc(int isActive);

    List<Feedback> findAllByParticipantIdInAndIsActiveOrderByFeedbackIdDesc(List<Long> empIds, int isActive);

    @Query("SELECT f FROM Feedback f WHERE f.feedbackDate BETWEEN :fromDate AND :toDate AND f.isActive = 1 ORDER BY feedbackId DESC")
    List<Feedback> findByDateRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("SELECT f FROM Feedback f WHERE f.participantId IN :empIds AND f.isActive = 1 AND f.feedbackDate BETWEEN :fromDate AND :toDate ORDER BY f.feedbackId DESC")
    List<Feedback> findAllActiveByParticipantsAndDateRange(
            @Param("empIds") List<Long> empIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("SELECT f FROM Feedback f WHERE f.participantId = :empId AND f.isActive = 1 AND f.feedbackDate BETWEEN :fromDate AND :toDate ORDER BY f.feedbackId DESC")
    List<Feedback> findAllActiveByParticipantAndDateRange(
            @Param("empId") Long empId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

}
