package org.joupen.commands.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.joupen.commands.GameCommand;
import org.joupen.domain.PlayerEntity;
import org.joupen.dto.PlayerDto;
import org.joupen.mapper.PlayerMapper;
import org.joupen.repository.PlayerRepository;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class InfoCommand implements GameCommand {
    private final CommandSender sender;
    private final PlayerRepository repo;
    private final PlayerMapper mapper;
    private final String targetName;

    @Override
    public void execute() {
        Optional<PlayerEntity> optional = repo.findByName(targetName);
        if (optional.isEmpty()) {
            sender.sendMessage(Component.text("Can't find player with name " + targetName, NamedTextColor.RED));
            log.warn("Player {} not found for info command", targetName);
            return;
        }

        PlayerEntity entity = optional.get();
        PlayerDto dto = mapper.entityToDto(entity);
        TextComponent textComponent = Component.text()
                .append(Component.text("Ник: ", NamedTextColor.GREEN))
                .append(Component.text(dto.getName(), NamedTextColor.BLUE)).appendNewline()
                .append(Component.text("UUID: ", NamedTextColor.GREEN))
                .append(Component.text(dto.getUuid().toString(), NamedTextColor.BLUE)).appendNewline()
                .append(Component.text("Последняя дата продления: ", NamedTextColor.GREEN))
                .append(Component.text(dto.getLastProlongDate().toString(), NamedTextColor.BLUE)).appendNewline()
                .append(Component.text("Активна до: ", NamedTextColor.GREEN))
                .append(Component.text(dto.getValidUntil().toString(), NamedTextColor.BLUE))
                .build();

        sender.sendMessage(textComponent);
        log.info("Displayed info for player {} to {}", targetName, sender.getName());
    }
}
