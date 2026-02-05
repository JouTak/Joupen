package commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.commands.GameCommand;
import org.joupen.commands.JoupenCommandFactory;
import org.joupen.events.SendPrivateMessageEvent;
import org.joupen.events.publishers.SimpleEventBus;
import org.joupen.repository.PlayerRepository;
import org.joupen.utils.EventUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JoupenCommandFactoryTest {

    private PlayerRepository repo;
    private SimpleEventBus bus;
    private List<String> inbox;

    @BeforeEach
    void setUp() {
        repo = mock(PlayerRepository.class);
        bus = new SimpleEventBus();
        inbox = new ArrayList<>();
        EventUtils.register(SendPrivateMessageEvent.class, evt -> {
            Component c = evt.message();
            inbox.add(PlainTextComponentSerializer.plainText().serialize(c));
        });
    }

    private BuildContext ctx(CommandSender sender, String label, String... args) {
        return BuildContext.builder()
                .sender(sender)
                .label(label)
                .argsTail(args)
                .playerRepository(repo)
                .transactionManager(null)
                .build();
    }

    @Test
    void emptyArgs_shouldReturnHelpCommand() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.getName()).thenReturn("Tester");

        GameCommand cmd = new JoupenCommandFactory().build(ctx(sender, "plugins/joupen"));
        assertNotNull(cmd);
        assertDoesNotThrow(cmd::execute);
    }

    @Test
    void unknownSub_shouldSendUnknownMessage() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.getName()).thenReturn("Tester");

        GameCommand cmd = new JoupenCommandFactory().build(ctx(sender, "plugins/joupen", "abracadabra"));
        cmd.execute();

        assertFalse(inbox.isEmpty());
        assertTrue(inbox.get(0).toLowerCase().contains("unknown subcommand"));
    }

    @Test
    void prolong_validate_shouldComplainWithoutPermissionAndArgs() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.getName()).thenReturn("Tester");
        when(sender.hasPermission("joupen.admin")).thenReturn(false);

        GameCommand cmd = new JoupenCommandFactory().build(ctx(sender, "plugins/joupen", "prolong"));
        cmd.execute();

        assertEquals(2, inbox.size(), "ожидаем 2 сообщения: нет прав + usage");
        assertTrue(inbox.get(0).toLowerCase().contains("don't have permission"));
        assertTrue(inbox.get(1).toLowerCase().contains("usage"));
    }
}
