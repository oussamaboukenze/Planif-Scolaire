package com.planifscolaire.servicereferentiel.mapper;

import com.planifscolaire.servicereferentiel.dto.EnseignantDto;
import com.planifscolaire.servicereferentiel.entite.Enseignant;

import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EnseignantMapper {
    Enseignant toEntity(EnseignantDto dto);

    EnseignantDto toDto(Enseignant entity);
}
