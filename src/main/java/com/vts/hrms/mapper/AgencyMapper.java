package com.vts.hrms.mapper;

import com.vts.hrms.dto.OrganizerIdDTO;
import com.vts.hrms.entity.Organizer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AgencyMapper extends EntityMapper<OrganizerIdDTO, Organizer> {

}