package org.joutak.loginpluginforjoutak.repository;

import org.joutak.loginpluginforjoutak.dto.PlayerDto;
import org.joutak.loginpluginforjoutak.inputoutput.JsonReaderImpl;
import org.joutak.loginpluginforjoutak.inputoutput.Reader;
import org.joutak.loginpluginforjoutak.utils.JoutakProperties;

import java.util.UUID;

public final class PlayerDtosUtils {
    public static PlayerDto findPlayerByName(String name) {
        Reader reader = new JsonReaderImpl(JoutakProperties.playersFilepath);
        PlayerDto playerDto = null;
        try {
            playerDto = reader.read().getPlayerDtoList().stream()
                    .filter(it -> it.getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
        } catch (Exception e) {
            // Логирование опущено для простоты, можно добавить при необходимости
        }
        return playerDto;
    }

    public static PlayerDto findPlayerByUuid(UUID uuid) {
        Reader reader = new JsonReaderImpl(JoutakProperties.playersFilepath);
        PlayerDto playerDto = null;
        try {
            playerDto = reader.read().getPlayerDtoList().stream()
                    .filter(it -> it.getUuid().equals(uuid))
                    .findFirst().orElse(null);
        } catch (Exception e) {
            // Логирование опущено для простоты, можно добавить при необходимости
        }
        return playerDto;
    }
}