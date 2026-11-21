package com.planifscolaire.utilisateurs.mapper;

import com.planifscolaire.utilisateurs.dto.UserDto;
import com.planifscolaire.utilisateurs.entite.User;

import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {
    User toEntity(UserDto dto);

    UserDto toDto(User entity);
}
