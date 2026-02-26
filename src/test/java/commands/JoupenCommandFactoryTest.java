package commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.commands.GameCommand;
import org.joupen.commands.JoupenCommandFactory;
import org.joupen.messaging.Messaging;
import org.joupen.messaging.Recipient;
import org.joupen.messaging.channels.MessageChannel;
import org.joupen.repository.PlayerRepository;
import org.joupen.service.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JoupenCommandFactoryTest {

    private PlayerRepository repo;
    private PlayerService playerService;
    private List<String> inbox;

    @BeforeEach
    void setUp() {
        repo = mock(PlayerRepository.class);
        playerService = mock(PlayerService.class);
        inbox = new ArrayList<>();

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
                inbox.add(PlainTextComponentSerializer.plainText().serialize(message));
            }
        });
    }

    private BuildContext ctx(CommandSender sender, String label, String... args) {
        return BuildContext.builder()
                .sender(sender)
                .label(label)
                .commandArgsWithName(args)
                .playerRepository(repo)
                .playerService(playerService)
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
    void prolong_noPermission_shouldDeny() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.getName()).thenReturn("Tester");
        when(sender.hasPermission("joupen.admin")).thenReturn(false);

        GameCommand cmd = new JoupenCommandFactory().build(ctx(sender, "plugins/joupen", "prolong", "SomePlayer"));
        cmd.execute();

        assertFalse(inbox.isEmpty());
        assertTrue(inbox.get(0).toLowerCase().contains("permission"));
    }

    @Test
    void prolong_noArgs_shouldShowUsage() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.getName()).thenReturn("Tester");
        when(sender.hasPermission("joupen.admin")).thenReturn(true);

        GameCommand cmd = new JoupenCommandFactory().build(ctx(sender, "plugins/joupen", "prolong"));
        cmd.execute();

        assertFalse(inbox.isEmpty());
        assertTrue(inbox.get(0).toLowerCase().contains("usage"));
    }
}
