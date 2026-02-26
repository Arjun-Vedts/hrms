package com.vts.hrms.mapper;

import com.vts.hrms.dto.FeedbackDTO;
import com.vts.hrms.entity.Feedback;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FeedbackMapper  extends EntityMapper<FeedbackDTO, Feedback> {
}
