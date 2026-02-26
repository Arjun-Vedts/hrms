package com.vts.hrms.mapper;

import com.vts.hrms.dto.CalendarDTO;
import com.vts.hrms.entity.Calendar;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CalendarMapper extends EntityMapper<CalendarDTO, Calendar> {
}
