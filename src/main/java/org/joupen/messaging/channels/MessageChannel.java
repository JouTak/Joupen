package org.joupen.messaging.channels;

import net.kyori.adventure.text.Component;
import org.joupen.messaging.Recipient;

/**
 * Channel = транспорт доставки (чат, discord, лог-файл, actionbar и т.д.)
 */
public interface MessageChannel {
    String id();

    void send(Recipient recipient, Component message);
}
