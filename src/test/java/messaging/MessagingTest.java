package messaging;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.joupen.messaging.Messaging;
import org.joupen.messaging.Recipient;
import org.joupen.messaging.channels.MessageChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class MessagingTest {

    private final List<String> out = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // isolate static state
        try {
            var m = Messaging.class.getDeclaredMethod("resetForTests");
            m.setAccessible(true);
            m.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Messaging.registerChannel(new MessageChannel() {
            @Override
            public String id() {
                return "chat";
            }

            @Override
            public void send(Recipient recipient, Component message) {
                out.add(recipient.kind() + ":" + PlainTextComponentSerializer.plainText().serialize(message));
            }
        });
    }

    @Test
    void reply_shouldSendToSenderRecipient() {
        CommandSender sender = mock(CommandSender.class);
        Messaging.reply(sender, Component.text("hi"));

        assertEquals(1, out.size());
        assertEquals("SENDER:hi", out.get(0));
    }

    @Test
    void broadcast_shouldSendToBroadcastRecipient() {
        Messaging.broadcast(Component.text("hello"));

        assertEquals(1, out.size());
        assertEquals("BROADCAST:hello", out.get(0));
    }

    @Test
    void send_unknownChannel_shouldNotThrow() {
        assertDoesNotThrow(() -> Messaging.send("nope", Recipient.broadcast(), Component.text("x")));
        assertTrue(out.isEmpty());
    }
}
