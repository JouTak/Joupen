package org.joutak.loginpluginforjoutak.logic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joutak.loginpluginforjoutak.dto.PlayerDto;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
//todo по-моему от этого класса надо бы избавиться и принимать везде нормальный  List<PlayerDto>
public class PlayerDtos {

    List<PlayerDto> playerDtoList;

}
