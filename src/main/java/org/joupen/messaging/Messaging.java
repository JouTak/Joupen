package org.joupen.messaging;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.joupen.messaging.channels.BukkitChatChannel;
import org.joupen.messaging.channels.MessageChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Фасад отправки сообщений.
 * <p>
 * Важно: тут больше нет "private/broadcast channels".
 * Есть transport "chat" (BukkitChatChannel), а кто получит — задаётся Recipient'ом.
 */
@Slf4j
public final class Messaging {
    private static final Map<String, MessageChannel> CHANNELS = new ConcurrentHashMap<>();
    private static boolean initialized;

    private Messaging() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        registerChannel(new BukkitChatChannel());

        log.info("Messaging initialized: channel registered {}", BukkitChatChannel.ID);
    }

    static void resetForTests() {
        CHANNELS.clear();
        initialized = false;
    }

    public static void registerChannel(MessageChannel channel) {
        CHANNELS.put(channel.id(), channel);
        log.info("Messaging channel registered: {}", channel.id());
    }

    public static void send(String channelId, Recipient recipient, Component message) {
        MessageChannel ch = CHANNELS.get(channelId);
        if (ch != null) {
            try {
                ch.send(recipient, message);
            } catch (Exception e) {
                log.error("Failed to send via channel {}: {}", channelId, e.getMessage(), e);
            }
        } else {
            log.warn("No channel registered for id {}", channelId);
        }
    }

    public static void reply(CommandSender sender, Component message) {
        send(BukkitChatChannel.ID, Recipient.sender(sender), message);
    }

    public static void broadcast(Component message) {
        send(BukkitChatChannel.ID, Recipient.broadcast(), message);
    }
}
