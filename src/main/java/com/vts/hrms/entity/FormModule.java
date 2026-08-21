package com.vts.hrms.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import java.time.LocalDateTime;


@Entity
@Table(name = "hrms_form_module")
@Data
@Builder
@NoArgsConstructor
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FormModule  {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "form_module_id", nullable = false)
    private Long formModuleId;


    @Column(name = "form_module_name", length = 255)
    private String formModuleName;

    @Column(name = "hindi_form_module_name", length = 255)
    private String hindiFormModuleName;


    @Column(name = "module_icon", length = 255)
    private String moduleIcon;


    @Column(name = "module_url", length = 255)
    private String moduleUrl;


    @Column(name = "serial_no", nullable = false)
    private Integer serialNo;


    @Column(name = "is_active", nullable = false)
    private Integer isActive;


    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_date")
    private LocalDateTime createdDate;


    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;


    public FormModule(Long formModuleId, String formModuleName,String hindiFormModuleName, String moduleIcon, String moduleUrl, Integer serialNo, Integer isActive, String createdBy, LocalDateTime createdDate, String modifiedBy, LocalDateTime modifiedDate) {
        this.formModuleId = formModuleId;
        this.formModuleName = formModuleName;
        this.hindiFormModuleName = hindiFormModuleName;
        this.moduleIcon = moduleIcon;
        this.moduleUrl = moduleUrl;
        this.serialNo = serialNo;
        this.isActive = isActive;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.modifiedBy = modifiedBy;
        this.modifiedDate = modifiedDate;
    }
}
