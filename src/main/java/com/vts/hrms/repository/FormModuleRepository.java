package com.vts.hrms.repository;


import com.vts.hrms.entity.FormModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FormModuleRepository extends JpaRepository<FormModule, Long> {

    @Query(value = """
            SELECT DISTINCT a.*
            FROM hrms_form_module a
            JOIN hrms_form_detail b ON a.form_module_id = b.form_module_id
            JOIN hrms_form_role_access c ON b.form_detail_id = c.form_detail_id
            JOIN role_security d ON c.role_id = d.role_id
            WHERE a.is_active = 1 AND c.is_active = 1 AND c.for_view="Y"
            AND d.role_name = :roleName
            ORDER BY a.serial_no
            """, nativeQuery = true)
    List<FormModule> findDistinctFormModulesByRoleId(@Param("roleName") String roleName);

    @Query(value = "SELECT form_module_id , form_module_name,hindi_form_module_name FROM hrms_form_module  WHERE is_active=1",
            nativeQuery = true)
    public List<Object[]> getformModulelist();

}
