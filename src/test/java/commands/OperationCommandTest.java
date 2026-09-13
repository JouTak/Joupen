package commands;

import be.seeseemelk.mockbukkit.MockBukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.commands.JoupenCommandFactory;
import org.joupen.domain.OperationType;
import org.joupen.messaging.Messaging;
import org.joupen.messaging.Recipient;
import org.joupen.messaging.channels.MessageChannel;
import org.joupen.repository.impl.PlayerRepositoryFileImpl;
import org.joupen.service.PlayerService;
import org.joupen.utils.EventUtils;
import org.joupen.utils.JoupenProperties;
import org.joupen.utils.OperationMessages;
import org.joupen.utils.ReflectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperationCommandTest {
    @TempDir
    Path directory;
    private PlayerRepositoryFileImpl repo;
    private PlayerService service;
    private CommandSender sender;
    private final List<String> inbox = new ArrayList<>();

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        EventUtils.reset();
        ReflectionUtils.init();
        OperationMessages.configure(Map.of());
        JoupenProperties.playersFilepath = directory.resolve("player.json").toString();
        repo = new PlayerRepositoryFileImpl();
        service = new PlayerService(repo);
        sender = mock(CommandSender.class);
        when(sender.getName()).thenReturn("Admin");
        when(sender.hasPermission("joupen.admin")).thenReturn(true);
        Messaging.registerChannel(new MessageChannel() {
            public String id() { return "chat"; }
            public void send(Recipient recipient, Component message) {
                inbox.add(PlainTextComponentSerializer.plainText().serialize(message));
            }
        });
    }

    @AfterEach
    void tearDown() {
        OperationMessages.configure(Map.of());
        EventUtils.reset();
        MockBukkit.unmock();
    }

    @Test
    void adjustmentUndoAndHistoryWorkThroughCommandFactory() {
        execute("prolong", "Player", "10d", "Initial", "purchase");
        var end = repo.findByName("Player").orElseThrow().getValidUntil();
        execute("adjust", "Player", "-3d", "Wrong", "amount");
        var adjustment = service.getOperations().history("Player", 1).get(0);
        assertEquals(end.minusDays(3), adjustment.getAfter().getValidUntil());
        assertEquals("Admin", adjustment.getMetadata().initiator());
        assertEquals("Wrong amount", adjustment.getMetadata().reason());
        execute("undo", "Player", adjustment.getId().toString(), "Correction");
        assertEquals(end, repo.findByName("Player").orElseThrow().getValidUntil());
        execute("history", "Player");
        assertTrue(inbox.stream().anyMatch(line -> line.contains("MANUAL_ADJUSTMENT -3d") && line.contains("Admin")));
        assertTrue(inbox.stream().anyMatch(line -> line.contains("undo=" + adjustment.getId())));
    }

    @Test
    void externalIdPreventsDuplicateCommandsAndConflictingPayloads() {
        execute("gift", "Player", "3d", "Event", "--source=script", "--external-id=event-123");
        execute("gift", "Player", "3d", "Event", "--source=script", "--external-id=event-123");
        assertEquals(1, service.getOperations().history("Player", 1).size());
        assertTrue(inbox.get(inbox.size() - 1).contains("Повтор пропущен"));
        execute("gift", "Player", "5d", "--source=script", "--external-id=event-123");
        assertEquals(1, service.getOperations().history("Player", 1).size());
        assertTrue(inbox.get(inbox.size() - 1).contains("другого запроса"));
    }

    @Test
    void compensationHasItsOwnTypeAndLastUndoDoesNotNeedAnId() {
        execute("compensate", "Player", "2h", "Downtime");
        assertEquals(OperationType.COMPENSATION, service.getOperations().history("Player", 1).get(0).getType());
        execute("undo", "Player");
        assertEquals(-Duration.ofHours(2).getSeconds(), service.getOperations().history("Player", 1).get(0).getDurationSeconds());
    }

    @Test
    void newCommandsRequireAdminPermission() {
        when(sender.hasPermission("joupen.admin")).thenReturn(false);
        execute("adjust", "Player", "3d");
        execute("compensate", "Player", "3d");
        execute("undo", "Player");
        execute("history", "Player");
        assertEquals(4, inbox.stream().filter(line -> line.contains("permission")).count());
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void malformedDurationDoesNotTurnIntoAPositiveGrant() {
        execute("prolong", "Player", "-3d");
        execute("adjust", "Player", "-3dgarbage");
        execute("gift", "Player", "0d");
        assertTrue(repo.findAll().isEmpty());
        assertEquals(3, inbox.size());
    }

    @Test
    void operationRepliesCanBeConfigured() {
        OperationMessages.configure(Map.of("operation-applied", "Applied {id}: {duration}"));
        execute("adjust", "Player", "+12h");
        assertEquals("Applied 1: 12h", inbox.get(0));
    }

    private void execute(String... args) {
        new JoupenCommandFactory().build(BuildContext.builder().sender(sender).label("joupen")
                .commandArgsWithName(args).playerRepository(repo).playerService(service).build()).execute();
    }
}
