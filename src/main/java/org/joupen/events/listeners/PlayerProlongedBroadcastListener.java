package org.joupen.events.listeners;

import net.kyori.adventure.text.Component;
import org.joupen.events.PlayerProlongedEvent;
import org.joupen.messaging.Messaging;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PlayerProlongedBroadcastListener implements Consumer<PlayerProlongedEvent> {

    @Override
    public void accept(PlayerProlongedEvent event) {
        String durationText = formatDurationRu(event.duration());

        String msg = event.gift()
                ? "Игрок " + event.player().getName() + " получил подарок " + durationText + "!"
                : "Игрок " + event.player().getName() + " продлил проходку " + durationText + "!";

        Messaging.broadcast(Component.text(msg));
    }

    private static String formatDurationRu(Duration duration) {
        if (duration == null) return "на 0 минут";

        long totalSeconds = Math.max(0, duration.getSeconds());

        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;

        List<String> parts = new ArrayList<>(3);
        if (days > 0) parts.add(days + " " + pluralRu(days, "день", "дня", "дней"));
        if (hours > 0) parts.add(hours + " " + pluralRu(hours, "час", "часа", "часов"));
        if (minutes > 0 || parts.isEmpty()) parts.add(minutes + " " + pluralRu(minutes, "минута", "минуты", "минут"));

        return "на " + String.join(" ", parts);
    }

    private static String pluralRu(long n, String one, String twoToFour, String fivePlus) {
        long nAbs = Math.abs(n);
        long mod100 = nAbs % 100;
        if (mod100 >= 11 && mod100 <= 14) return fivePlus;

        long mod10 = nAbs % 10;
        if (mod10 == 1) return one;
        if (mod10 >= 2 && mod10 <= 4) return twoToFour;
        return fivePlus;
    }
}