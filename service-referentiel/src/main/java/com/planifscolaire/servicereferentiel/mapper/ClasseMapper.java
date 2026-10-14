package com.planifscolaire.servicereferentiel.mapper;

import com.planifscolaire.servicereferentiel.dto.ClasseDto;
import com.planifscolaire.servicereferentiel.entite.Classe;

import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ClasseMapper {
    Classe toEntity(ClasseDto dto);

    ClasseDto toDto(Classe entity);
}
