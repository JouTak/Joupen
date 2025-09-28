package org.joupen.events;

import org.joupen.domain.PlayerEntity;

import java.time.Duration;

public record PlayerProlongedEvent(PlayerEntity player, boolean gift, Duration duration) implements Event {
}
