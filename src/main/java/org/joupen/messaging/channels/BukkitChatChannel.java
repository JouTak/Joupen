package org.joupen.messaging.channels;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.joupen.messaging.Recipient;

/**
 * Один канал "chat" умеет отправлять:
 * - в broadcast (всем игрокам + консоль)
 * - конкретному sender
 */
public class BukkitChatChannel implements MessageChannel {
    public static final String ID = "chat";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void send(Recipient recipient, Component message) {
        switch (recipient.kind()) {
            case SENDER -> {
                CommandSender sender = recipient.requireSender();
                sender.sendMessage(message);
            }
            case BROADCAST -> {
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(message));
                ConsoleCommandSender console = Bukkit.getConsoleSender();
                if (console != null) console.sendMessage(message);
            }
        }
    }
}
