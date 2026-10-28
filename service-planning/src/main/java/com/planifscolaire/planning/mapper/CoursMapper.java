package com.planifscolaire.planning.mapper;

import com.planifscolaire.planning.dto.CoursDto;
import com.planifscolaire.planning.dto.CreateCoursDto;
import com.planifscolaire.planning.entite.Cours;

import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CoursMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "nbAbsencesSignalees", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    Cours toEntity(CreateCoursDto dto);

    CoursDto toDto(Cours entity);
}
