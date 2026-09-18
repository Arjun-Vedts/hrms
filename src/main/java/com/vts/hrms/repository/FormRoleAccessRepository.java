package com.vts.hrms.repository;

import com.vts.hrms.entity.FormRoleAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface FormRoleAccessRepository extends JpaRepository<FormRoleAccess,Long> {


    @Query(value = """
            SELECT b.form_role_access_id, a.form_detail_id, a.form_module_id, a.form_disp_name, b.is_active , b.for_view, b.for_add, b.for_edit, b.for_delete, b.role_id
            FROM (SELECT fd.form_detail_id, fd.form_module_id, fd.form_disp_name
            FROM hrms_form_detail fd
            WHERE fd.is_active = 1
            AND CASE WHEN :formModuleId <> '0' THEN fd.form_module_id = :formModuleId ELSE 1 = 1 END) AS a
            LEFT JOIN (SELECT b.form_role_access_id, b.form_detail_id AS 'detailid', b.role_id, b.is_active , b.for_view, b.for_add, b.for_edit, b.for_delete
            FROM hrms_form_detail a, hrms_form_role_access b, role_security c
            WHERE a.form_detail_id = b.form_detail_id
            AND b.role_id = c.role_id AND c.role_name = :roleName
            AND CASE WHEN :formModuleId <> '0' THEN a.form_module_id = :formModuleId ELSE 1 = 1 END) AS b
            ON a.form_detail_id = b.detailid
            """,
            nativeQuery = true)
    List<Object[]> getformroleAccessList(@Param("roleName") String roleName,
                                         @Param("formModuleId") String formModuleId);

    @Query(value = "SELECT COUNT(form_role_access_id) " +
            "FROM hrms_form_role_access " +
            "WHERE role_id = :RoleId " +
            "AND form_detail_id = :formDetailId",
            nativeQuery = true)
    long countByFormRoleIdAndDetailId(@Param("RoleId") String roleId,
                                      @Param("formDetailId") String detailsId);


    public Optional<FormRoleAccess> findByRoleIdAndFormDetailId(Long roleId, Long formDetailId);
}
