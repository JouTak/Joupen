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
       Component nameComponent = Component.text(playerDto.getName(), NamedTextColor.BLUE)
                .clickEvent(ClickEvent.copyToClipboard(playerDto.getName()))
                .hoverEvent(Component.text("Нажми, чтобы скопировать ник", NamedTextColor.GRAY));

        Component uuidComponent = Component.text(playerDto.getUuid().toString(), NamedTextColor.BLUE)
                .clickEvent(ClickEvent.copyToClipboard(playerDto.getUuid().toString()))
                .hoverEvent(Component.text("Нажми, чтобы скопировать UUID", NamedTextColor.GRAY));
        TextComponent textComponent = Component.text()
                .append(Component.text(args.length > 1 ? "Ник Игрока:" : "Твой Ник:", NamedTextColor.GREEN))
                .append(nameComponent)
                .appendNewline()
                .append(Component.text(args.length > 1 ? "UUID Игрока:" : "Твой UUID:", NamedTextColor.GREEN))
                .append(uuidComponent)
                .appendNewline()
                .append(Component.text("Последняя дата продления проходки: ", NamedTextColor.GREEN))
                .append(Component.text(playerDto.getLastProlongDate().toString(), NamedTextColor.BLUE))
                .appendNewline()
                .append(Component.text("Проходка активна до: ", NamedTextColor.GREEN))
                .append(Component.text(playerDto.getValidUntil().toString(), NamedTextColor.BLUE))
                .build();

        sender.sendMessage(textComponent);
        log.info("Displayed info for player {} to {}", targetName, sender.getName());
    }
}
