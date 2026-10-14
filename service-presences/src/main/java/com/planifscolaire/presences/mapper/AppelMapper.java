package com.planifscolaire.presences.mapper;

import com.planifscolaire.presences.dto.AppelDto;
import com.planifscolaire.presences.entite.Appel;

import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AppelMapper {
    AppelDto toDto(Appel entity);
}
