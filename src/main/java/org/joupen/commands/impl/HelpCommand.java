package org.joupen.commands.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandAlias;
import org.joupen.commands.GameCommand;

@CommandAlias(name = "help")
public class HelpCommand implements GameCommand {
    private final CommandSender sender;

    public HelpCommand(BuildContext buildContext) {
        this.sender = buildContext.getSender();
    }

    @Override
    public void execute() {
        TextComponent.Builder text = Component.text()
                .append(Component.text("Joupen", NamedTextColor.GOLD)).appendNewline()
                .append(Component.text("Вайтлист плагин для ДжоуТека", NamedTextColor.GOLD)).appendNewline()
                .append(Component.text("Инструкция:", NamedTextColor.DARK_GREEN)).appendNewline()
                .append(Component.text("/joupen help", NamedTextColor.GREEN))
                .append(Component.text(" - показывает эту страницу", NamedTextColor.DARK_GREEN)).appendNewline()
                .append(Component.text("/joupen info [player]", NamedTextColor.GREEN))
                .append(Component.text(" - инфо о проходке [другого игрока]", NamedTextColor.DARK_GREEN)).appendNewline()
                .append(Component.text("/joupen link", NamedTextColor.GREEN))
                .append(Component.text(" - выводит ссылку на оплату проходочки", NamedTextColor.DARK_GREEN)).appendNewline()
                .appendNewline();

        if (sender.hasPermission("joupen.admin")){
            text
                    .append(Component.text("/joupen prolong <player|all> [duration] [reason]", NamedTextColor.GREEN))
                    .append(Component.text(" - продлить проходочку. Default: 1mo", NamedTextColor.DARK_GREEN)).appendNewline()
                    .append(Component.text("/joupen gift <player|all> [duration] [reason]", NamedTextColor.GREEN))
                    .append(Component.text(" - подарить проходочку. Default: 1mo", NamedTextColor.DARK_GREEN)).appendNewline()

                    .append(Component.text("/joupen adjust <player> <+/-duration> [reason]", NamedTextColor.GREEN)).appendNewline()
                    .append(Component.text("- скорректировать проходочку.", NamedTextColor.DARK_GREEN)).appendNewline()
                    .append(Component.text("/joupen compensate <player> <duration> [reason]", NamedTextColor.GREEN)).appendNewline()
                    .append(Component.text("- компенсировать проходочку.", NamedTextColor.DARK_GREEN)).appendNewline()
                    .append(Component.text("/joupen undo <player> [last|id] [reason]", NamedTextColor.GREEN)).appendNewline()
                    .append(Component.text("- отменить действие из истории.", NamedTextColor.DARK_GREEN)).appendNewline()
                    .append(Component.text("/joupen history <player> [page]", NamedTextColor.GREEN)).appendNewline()
                    .append(Component.text("- просмотреть историю действий.", NamedTextColor.DARK_GREEN)).appendNewline()
                    .append(Component.text("/joupen addAllToWhitelist <file> <days>", NamedTextColor.GREEN))
                    .append(Component.text(" - импорт никнеймов из файла", NamedTextColor.BLUE)).appendNewline()
                    .appendNewline();
        }
        text.append(Component.text(
                       "Developed by JouTak team: https://github.com/JouTak/Joupen", NamedTextColor.DARK_AQUA)
                );




        sender.sendMessage(text.build());
    }
}
