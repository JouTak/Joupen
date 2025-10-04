package org.joupen.events.publishers;

import org.joupen.events.Event;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SimpleEventBus implements EventPublisher {
    private final ConcurrentHashMap<Class<? extends Event>, CopyOnWriteArrayList<Consumer<? extends Event>>> listeners = new ConcurrentHashMap<>();

    public <T extends Event> void register(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void reset() {
        listeners.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void publish(Object event) {
        if (!(event instanceof Event)) {
            throw new IllegalArgumentException("Only Event types are supported, got: " + event.getClass());
        }

        List<Consumer<? extends Event>> typedListeners = listeners.get(event.getClass());
        if (typedListeners != null) {
            for (Consumer<? extends Event> l : typedListeners) {
                ((Consumer<Event>) l).accept((Event) event);
            }
        }
    }
}

