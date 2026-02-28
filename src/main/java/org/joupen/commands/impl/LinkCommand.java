package org.joupen.commands.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandAlias;
import org.joupen.commands.GameCommand;

@CommandAlias(name = "link")
public class LinkCommand implements GameCommand {
    private final CommandSender sender;

    public LinkCommand(BuildContext buildContext) {
        this.sender = buildContext.getSender();
    }

    @Override
    public void execute() {
        TextComponent textComponent = Component.text()
                .append(Component.text("Joupen", NamedTextColor.GOLD)).appendNewline()
                .append(Component.text("Ссылка на оплату проходочки ДжоуТека:", NamedTextColor.DARK_GREEN)).appendNewline()
                .append(Component.text("https://clck.ru/3EEMC9", NamedTextColor.DARK_GREEN))
                .append(Component.text(" *КЛИК*", NamedTextColor.GOLD))
                .clickEvent(ClickEvent.openUrl("https://forms.yandex.ru/u/6515e3dcd04688fca3cc271b/"))
                .build();
        sender.sendMessage(textComponent);
    }
}
