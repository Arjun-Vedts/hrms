package com.vts.hrms.mapper;

import com.vts.hrms.dto.SignAuthRoleDTO;
import com.vts.hrms.entity.SignAuthRole;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SignAuthRoleMapper extends EntityMapper<SignAuthRoleDTO, SignAuthRole> {
}
