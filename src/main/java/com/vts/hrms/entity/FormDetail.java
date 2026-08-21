package com.vts.hrms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "hrms_form_detail")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FormDetail {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "form_detail_id", nullable = false)
    private Long formDetailId;

    @NotNull
    @Column(name = "form_module_id", nullable = false)
    private Long formModuleId;

    @NotNull
    @Size(max = 100)
    @Column(name = "form_name", length = 100, nullable = false)
    private String formName;

    @Size(max = 255)
    @Column(name = "form_url", length = 255)
    private String formUrl;

    @Size(max = 100)
    @Column(name = "form_disp_name", length = 100)
    private String formDispName;

    @Size(max = 100)
    @Column(name = "hindi_form_disp_name", length = 100)
    private String hindiFormDispName;

    @NotNull
    @Column(name = "form_admin", nullable = false)
    private Integer formAdmin;

    @NotNull
    @Column(name = "form_serial_no", nullable = false)
    private Integer formSerialNo;

    @Size(max = 255)
    @Column(name = "form_color", length = 255)
    private String formColor;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Integer isActive;

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

}
