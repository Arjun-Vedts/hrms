package com.vts.hrms.mapper;

import com.vts.hrms.dto.RequisitionDTO;
import com.vts.hrms.entity.Requisition;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RequisitionMapper extends EntityMapper<RequisitionDTO, Requisition> {
}
