package org.joupen.inputoutput;

import org.joupen.dto.PlayerDto;
import org.joupen.dto.PlayerDtos;

public interface Writer {

    void write(PlayerDtos playerDtos);

    void addNew(PlayerDto playerDto);

}
