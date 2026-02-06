package org.joupen.messaging;

import org.bukkit.command.CommandSender;

import java.util.Objects;

/**
 * Кому отправлять.
 * Kind = аудитория/адресат.
 */
public record Recipient(Kind kind, CommandSender sender) {

    public enum Kind {
        BROADCAST,
        SENDER
    }

    public static Recipient broadcast() {
        return new Recipient(Kind.BROADCAST, null);
    }

    public static Recipient sender(CommandSender sender) {
        Objects.requireNonNull(sender, "sender");
        return new Recipient(Kind.SENDER, sender);
    }

    public CommandSender requireSender() {
        if (sender == null) {
            throw new IllegalStateException("Recipient sender is null, kind=" + kind);
        }
        return sender;
    }
}
