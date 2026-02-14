package org.joupen.commands.impl;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.joupen.commands.GameCommand;

@RequiredArgsConstructor
public class HelpCommand implements GameCommand {
    private final CommandSender sender;

    @Override
    public void execute() {
        TextComponent text = Component.text()
                .append(Component.text("Joupen", NamedTextColor.GOLD)).appendNewline()
                .append(Component.text("Вайтлист плагин для ДжоуТека", NamedTextColor.GOLD)).appendNewline()
                .append(Component.text("Help:", NamedTextColor.BLUE)).appendNewline()
                .append(Component.text("/joupen help", NamedTextColor.GREEN))
                .append(Component.text(" - показывает эту страницу", NamedTextColor.BLUE)).appendNewline()
                .append(Component.text("/joupen prolong <player|all> [duration]", NamedTextColor.GREEN))
                .append(Component.text(" - продлевает игрока. Default: 1mo", NamedTextColor.BLUE)).appendNewline()
                .append(Component.text("/joupen gift <player|all> [duration]", NamedTextColor.GREEN))
                .append(Component.text(" - бесплатное продление. Default: 1mo", NamedTextColor.BLUE)).appendNewline()
                .append(Component.text("/joupen info [player]", NamedTextColor.GREEN))
                .append(Component.text(" - инфо о проходке (чужого — только админ)", NamedTextColor.BLUE)).appendNewline()
                .append(Component.text("/joupen addAllToWhitelist <file> <days>", NamedTextColor.GREEN))
                .append(Component.text(" - импорт никнеймов из файла", NamedTextColor.BLUE)).appendNewline()
                .append(Component.text("Developed by Lapitaniy The Грыбник", NamedTextColor.DARK_AQUA))
                .build();

        sender.sendMessage(text);
    }
}
