package org.joupen.utils;

import org.joupen.events.Event;
import org.joupen.events.PlayerProlongedEvent;
import org.joupen.events.listeners.PlayerProlongedBroadcastListener;
import org.joupen.events.publishers.SimpleEventBus;

import java.util.function.Consumer;

public class EventUtils {
    private static final SimpleEventBus eventBus = new SimpleEventBus();

    public static void publish(Event event) {
        eventBus.publish(event);
    }

    public static  <T extends Event> void register(Class<T> eventType, Consumer<T> listener) {
        eventBus.register(eventType, listener);
    }
}
