package com.vts.hrms.repository;

import com.vts.hrms.dto.LoginEmployeeDto;
import com.vts.hrms.dto.UserListRowDTO;
import com.vts.hrms.dto.UserResponseDTO;
import com.vts.hrms.entity.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginRepository extends JpaRepository<Login, Long> {

    @Query(value = """
            SELECT
                   a.login_id  AS loginId,
                   c.role_id   AS roleId,
                   a.emp_id    AS empId,
                   NULL   AS  divisionId,
                   a.username   AS username,
                   NULL AS employeeName,
                   NULL AS designationName,
                   c.role_name AS roleName,
                   NULL AS divisionName
                   FROM login a
                   LEFT JOIN login_role_security b ON b.login_id = a.login_id
                   LEFT JOIN role_security c ON c.role_id = b.role_id
                   WHERE a.is_active = 1 ;""", nativeQuery = true)
    List<UserListRowDTO> getUserList();

    boolean existsByUsernameIgnoreCase(String username);

    @Query(value = "SELECT DISTINCT d.emp_id, f.role_name, d.login_id ,f.role_id,f.hindi_role_name " +
            "FROM login d " +
            "JOIN login_role_security e ON e.login_id=d.login_id " +
            "JOIN role_security f ON f.role_id=e.role_id " +
            "WHERE d.username=:username " +
            "AND d.is_active=1 LIMIT 1", nativeQuery = true)
    LoginEmployeeDto findByUserName(@Param("username") String username);

    Login findByUsernameAndIsActive(String username, int isActive);

    boolean existsByUsernameAndIsActive(String username, int isActive);

    @Query(value = "SELECT DISTINCT d.emp_id, f.role_name, d.login_id ,f.role_id,f.hindi_role_name " +
            "FROM login d " +
            "JOIN login_role_security e ON e.login_id=d.login_id " +
            "JOIN role_security f ON f.role_name=:roleDirector AND f.role_id=e.role_id " +
            "WHERE d.is_active=1 LIMIT 1", nativeQuery = true)
    LoginEmployeeDto findEmployeeByRoleName(@Param("roleDirector") String roleDirector);

    Login findByUsername(String username);
}

