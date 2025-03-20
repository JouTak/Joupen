package org.joutak.loginpluginforjoutak.mapper;

import org.joutak.loginpluginforjoutak.domain.PlayerEntity;
import org.joutak.loginpluginforjoutak.dto.PlayerDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;


@Mapper(componentModel = "default")
public interface PlayerMapper {

    PlayerDto entityToDto(PlayerEntity source);

    PlayerEntity dtoToEntity(PlayerDto destination);

    void updateEntityFromDto(PlayerDto dto, @MappingTarget PlayerEntity entity);
}

