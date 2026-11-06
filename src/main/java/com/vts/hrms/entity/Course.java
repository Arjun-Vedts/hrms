package com.vts.hrms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "hrms_course")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Course implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_code")
    private String courseCode;

    @Column(name = "course_level")
    private String courseLevel;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "course_type_id")
    private Long courseTypeId;

    @Column(name = "organizer_id")
    private Long organizerId;

    @Column(name = "eligibility_id")
    private Long eligibilityId;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "offline_registration_fee")
    private BigDecimal offlineRegistrationFee;

    @Column(name = "online_registration_fee")
    private BigDecimal onlineRegistrationFee;

    @Column(name = "venue")
    private String venue;

    @Column(name = "no_of_nomination")
    private Long noOfNomination;

    @Size(max = 255)
    @Column(name = "brochure_name", length = 255)
    private String brochureName;

    @Size(max = 100)
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Size(max = 100)
    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Integer isActive;


}
