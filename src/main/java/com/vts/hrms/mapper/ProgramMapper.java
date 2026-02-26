package com.vts.hrms.mapper;

import com.vts.hrms.dto.ProgramDTO;
import com.vts.hrms.entity.Program;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProgramMapper extends EntityMapper<ProgramDTO, Program> {
}
