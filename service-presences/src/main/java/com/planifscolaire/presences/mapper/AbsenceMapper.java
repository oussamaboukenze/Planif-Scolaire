package com.planifscolaire.presences.mapper;

import com.planifscolaire.presences.dto.AbsenceDto;
import com.planifscolaire.presences.entite.Absence;

import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AbsenceMapper {
    AbsenceDto toDto(Absence entity);
}
