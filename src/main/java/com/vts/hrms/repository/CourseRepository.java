package com.vts.hrms.repository;

import com.vts.hrms.dto.CourseDashboardDTO;
import com.vts.hrms.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByIsActive(int isActive);

    List<Course> findAllByOrganizerIdAndIsActive(Long orgId, int isActive);

    @Query("""
                SELECT new com.vts.hrms.dto.CourseDashboardDTO(
                    c.organizerId,
                    o.organizer,
                    COUNT(c.courseId)
                )
                FROM Course c
                JOIN Organizer o ON c.organizerId = o.organizerId
                WHERE c.isActive = 1
                AND c.fromDate >= :startDate
                AND c.toDate <= :endDate
                GROUP BY c.organizerId, o.organizer
            """)
    List<CourseDashboardDTO> getOrganizerWiseCourseCount(LocalDate startDate, LocalDate endDate);

    @Query("""
                SELECT c FROM Course c
                WHERE (:orgId <= 0 OR c.organizerId = :orgId)
                AND c.isActive = 1
                AND c.fromDate >= :fromDate
                AND c.toDate <= :toDate
                ORDER BY c.fromDate DESC
            """)
    List<Course> findCoursesByOrganizerAndDateRange(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
