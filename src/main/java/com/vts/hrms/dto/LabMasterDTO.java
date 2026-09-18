package com.vts.hrms.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LabMasterDTO {

    private Long labMasterId;
    private String labCode;
    private String labName;
    private String labUnitCode;
    private String labAddress;
    private String labCity;
    private String labPin;
    private String labTelNo;
    private String labFaxNo;
    private String labEmail;
    private String labAuthority;
    private Long labAuthorityId;
    private Long labDgId;
    private Long labSecyId;
    private String labRfpEmail;
    private String isCluster;
    private Long labId;
    private Long clusterId;
    private byte[] labLogo;
}
