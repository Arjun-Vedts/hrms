package com.vts.hrms.mapper;

import com.vts.hrms.dto.SignRoleAuthorityDTO;
import com.vts.hrms.entity.SignRoleAuthority;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SignRoleAuthorityMapper extends EntityMapper<SignRoleAuthorityDTO, SignRoleAuthority> {
}
