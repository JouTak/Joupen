package org.joupen.bukkit.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public class JoupenPassProlongedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID uuid;
    private final String name;
    private final boolean gift;
    private final Duration duration;
    private final LocalDateTime validUntil;

    public JoupenPassProlongedEvent(UUID uuid, String name, boolean gift, Duration duration, LocalDateTime validUntil) {
        this.uuid = uuid;
        this.name = name;
        this.gift = gift;
        this.duration = duration;
        this.validUntil = validUntil;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public boolean isGift() { return gift; }
    public Duration getDuration() { return duration; }
    public LocalDateTime getValidUntil() { return validUntil; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
