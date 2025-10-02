package org.joupen.events.listeners;

import org.joupen.events.SendPrivateMessageEvent;

import java.util.function.Consumer;

public class PrivateMessageListener implements Consumer<SendPrivateMessageEvent> {

    @Override
    public void accept(SendPrivateMessageEvent event) {
        event.sender().sendMessage(event.message());
    }
}
