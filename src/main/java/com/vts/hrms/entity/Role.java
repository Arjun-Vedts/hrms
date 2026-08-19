package com.vts.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "role_security")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_name")
    private String roleName;


    @Column(name = "hindi_role_name")
    private String hindiRoleName;



}