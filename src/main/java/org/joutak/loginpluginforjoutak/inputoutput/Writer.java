package org.joutak.loginpluginforjoutak.inputoutput;

import org.joutak.loginpluginforjoutak.dto.PlayerDto;
import org.joutak.loginpluginforjoutak.dto.PlayerDtos;

public interface Writer {

    void write(PlayerDtos playerDtos);

    void addNew(PlayerDto playerDto);

}
