package org.joupen.mapper;

import org.joupen.domain.PlayerEntity;
import org.joupen.dto.PlayerDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;


@Mapper(componentModel = "default")
public interface PlayerMapper {

    PlayerDto entityToDto(PlayerEntity source);

    PlayerEntity dtoToEntity(PlayerDto destination);

    void updateEntityFromDto(PlayerDto dto, @MappingTarget PlayerEntity entity);
}

