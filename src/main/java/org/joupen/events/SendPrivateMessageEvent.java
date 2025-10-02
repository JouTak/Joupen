package org.joupen.events;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public record SendPrivateMessageEvent(CommandSender sender, Component message) implements Event {
}



