package org.joupen.commands.impl;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandAlias;
import org.joupen.commands.GameCommand;
import org.joupen.domain.PlayerEntity;
import org.joupen.dto.PlayerDto;
import org.joupen.mapper.PlayerMapper;
import org.joupen.messaging.Messaging;
import org.joupen.repository.PlayerRepository;
import org.joupen.utils.TimeUtils;
import org.joupen.validation.CommandValidator;
import org.joupen.validation.Validator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@CommandAlias(name = "info", maxArgs = 1)
public class InfoCommand implements GameCommand, CommandValidator {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final CommandSender sender;
    private final PlayerRepository repo;
    private final PlayerMapper mapper;
    private final String targetName;
    private final boolean self;

    public InfoCommand(BuildContext buildContext) {
        this.sender = buildContext.getSender();
        this.repo = buildContext.getPlayerRepository();
        this.mapper = buildContext.getPlayerMapper() != null
                ? buildContext.getPlayerMapper()
                : PlayerMapper.INSTANCE;

        String[] args = buildContext.getArgs();
        this.targetName = args.length == 0 ? buildContext.getSender().getName() : args[0];
        this.self = args.length == 0;
    }

    @Override
    public List<Component> validate(BuildContext ctx, String[] args) {
        Validator v = Validator.of(ctx, args);
        if (args.length > 0) {
            v.permission("joupen.admin");
        }
        return v.check();
    }

    @Override
    public void execute() {
        Optional<PlayerEntity> optional = repo.findByName(targetName);
        if (optional.isEmpty()) {
            Messaging.reply(sender, Component.text("Can't find player with name " + targetName, NamedTextColor.RED));
            log.warn("Player {} not found for info command", targetName);
            return;
        }

        PlayerEntity entity = optional.get();
        PlayerDto dto = mapper.entityToDto(entity);

        Component nameComponent = Component.text(dto.getName(), NamedTextColor.BLUE)
                .clickEvent(ClickEvent.copyToClipboard(dto.getName()))
                .hoverEvent(Component.text("Нажми, чтобы скопировать ник", NamedTextColor.GRAY));

        Component uuidComponent = Component.text(dto.getUuid().toString(), NamedTextColor.BLUE)
                .clickEvent(ClickEvent.copyToClipboard(dto.getUuid().toString()))
                .hoverEvent(Component.text("Нажми, чтобы скопировать UUID", NamedTextColor.GRAY));

        TextComponent.Builder textComponent = Component.text()
                .append(Component.text(self ? "Твой Ник: " : "Ник Игрока: ", NamedTextColor.GREEN))
                .append(nameComponent)
                .appendNewline()
                .append(Component.text(self ? "Твой UUID: " : "UUID Игрока: ", NamedTextColor.GREEN))
                .append(uuidComponent)
                .appendNewline();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastProlong = dto.getLastProlongDate();
        LocalDateTime validUntil = dto.getValidUntil();
        boolean hasValidUntil = validUntil != null;
        boolean hasLastProlong = lastProlong != null;
        if (hasLastProlong){
            textComponent
                    .append(Component.text(hasValidUntil ? "Последняя дата продления проходки: " : "Последнее редактирование записи: ", NamedTextColor.GREEN))
                    .append(Component.text(lastProlong.format(FMT), NamedTextColor.DARK_GREEN))
                    .appendNewline();

            if (hasValidUntil) {
                TimeUtils.PassProgress progress = TimeUtils.calculatePassProgress(now, lastProlong, validUntil);
                String validUntilText = validUntil.format(FMT)
                        + " (" + progress.getDaysRemaining() + " дн., " + progress.getPercent() + "%)";
                textComponent
                        .append(Component.text("Проходка активна до: ", NamedTextColor.GREEN))
                        .append(Component.text(validUntilText, NamedTextColor.DARK_GREEN))
                        .appendNewline();
            }
            else{
                textComponent
                        .append(Component.text("Не имеет активной проходки", NamedTextColor.GREEN))
                        .appendNewline();
            }
        }
        else{
            textComponent
                    .append(Component.text("Неизвестна дата редактирования проходки", NamedTextColor.GREEN))
                    .appendNewline();
        }



        boolean approved = Boolean.TRUE.equals(dto.getApproved());
        textComponent.append(Component.text(
                        approved ? "Подтверждён": "Не подтверждён",
                        approved ? NamedTextColor.GREEN : NamedTextColor.RED
        ))
                .appendNewline();

        LocalDateTime temporaryAccessFrom = dto.getTemporaryAccessFrom();
        LocalDateTime temporaryAccessUntil = dto.getTemporaryAccessUntil();
        boolean hasWindow = temporaryAccessFrom != null && temporaryAccessUntil != null;
        if (hasWindow) {
            textComponent
                    .append(Component.text("Бесплатный доступ: "))
                    .append(Component.text(temporaryAccessFrom.format(FMT), NamedTextColor.GREEN))
                    .append(Component.text("--"))
                    .append(Component.text(temporaryAccessUntil.format(FMT), NamedTextColor.GREEN));
        }

        Messaging.reply(sender, textComponent.build());
        log.info("Displayed info for player {} to {}", targetName, sender.getName());
    }
}