package com.planifscolaire.planning.mapper;

import com.planifscolaire.planning.dto.CoursDtoFull;
import com.planifscolaire.planning.entite.Cours;

import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CoursFullMapper {
    @Mapping(target = "classe", ignore = true)
    @Mapping(target = "enseignant", ignore = true)
    CoursDtoFull toDto(Cours cours);
}
