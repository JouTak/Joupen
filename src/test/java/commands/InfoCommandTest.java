package commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.commands.impl.InfoCommand;
import org.joupen.domain.PlayerEntity;
import org.joupen.dto.PlayerDto;
import org.joupen.mapper.PlayerMapper;
import org.joupen.messaging.Messaging;
import org.joupen.messaging.Recipient;
import org.joupen.messaging.channels.MessageChannel;
import org.joupen.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InfoCommandTest {

    private PlayerRepository repo;
    private PlayerMapper mapper;
    private CommandSender sender;
    private List<String> messages;

    @BeforeEach
    void setUp() {
        repo = mock(PlayerRepository.class);
        mapper = mock(PlayerMapper.class);
        sender = mock(CommandSender.class);
        when(sender.getName()).thenReturn("TestSender");
        messages = new ArrayList<>();

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
                messages.add(PlainTextComponentSerializer.plainText().serialize(message));
            }
        });
    }

    @Test
    void execute_playerNotFound_shouldDisplayError() {
        when(repo.findByName("NonExistent")).thenReturn(Optional.empty());

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{"NonExistent"})
                .playerRepository(repo)
                .playerMapper(mapper)
                .build();

        InfoCommand cmd = new InfoCommand(ctx);
        cmd.execute();

        assertFalse(messages.isEmpty());
        assertTrue(messages.get(0).toLowerCase().contains("can't find"));
    }

    @Test
    void execute_selfInfo_shouldDisplayOwnData() {
        LocalDateTime now = LocalDateTime.now();
        PlayerEntity entity = new PlayerEntity(1L, UUID.randomUUID(), "TestSender",
                now.plusDays(30), now.minusDays(10), true);
        PlayerDto dto = PlayerDto.builder()
                .name("TestSender")
                .uuid(entity.getUuid())
                .validUntil(entity.getValidUntil())
                .lastProlongDate(entity.getLastProlongDate())
                .build();

        when(repo.findByName("TestSender")).thenReturn(Optional.of(entity));
        when(mapper.entityToDto(entity)).thenReturn(dto);

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{})
                .playerRepository(repo)
                .playerMapper(mapper)
                .build();

        InfoCommand cmd = new InfoCommand(ctx);
        cmd.execute();

        assertFalse(messages.isEmpty());
        String message = messages.get(0);
        assertTrue(message.contains("TestSender"));
        assertTrue(message.contains("дн."));
        assertTrue(message.contains("%"));
    }

    @Test
    void execute_otherPlayerInfo_shouldDisplayTheirData() {
        LocalDateTime now = LocalDateTime.now();
        PlayerEntity entity = new PlayerEntity(1L, UUID.randomUUID(), "OtherPlayer",
                now.plusDays(15), now.minusDays(5), true);
        PlayerDto dto = PlayerDto.builder()
                .name("OtherPlayer")
                .uuid(entity.getUuid())
                .validUntil(entity.getValidUntil())
                .lastProlongDate(entity.getLastProlongDate())
                .build();

        when(repo.findByName("OtherPlayer")).thenReturn(Optional.of(entity));
        when(mapper.entityToDto(entity)).thenReturn(dto);

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{"OtherPlayer"})
                .playerRepository(repo)
                .playerMapper(mapper)
                .build();

        InfoCommand cmd = new InfoCommand(ctx);
        cmd.execute();

        assertFalse(messages.isEmpty());
        assertTrue(messages.get(0).contains("OtherPlayer"));
    }

    @Test
    void execute_expiredPass_shouldShow0Days() {
        LocalDateTime now = LocalDateTime.now();
        PlayerEntity entity = new PlayerEntity(1L, UUID.randomUUID(), "ExpiredPlayer",
                now.minusDays(5), now.minusDays(35), true);
        PlayerDto dto = PlayerDto.builder()
                .name("ExpiredPlayer")
                .uuid(entity.getUuid())
                .validUntil(entity.getValidUntil())
                .lastProlongDate(entity.getLastProlongDate())
                .build();

        when(repo.findByName("ExpiredPlayer")).thenReturn(Optional.of(entity));
        when(mapper.entityToDto(entity)).thenReturn(dto);

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{"ExpiredPlayer"})
                .playerRepository(repo)
                .playerMapper(mapper)
                .build();

        InfoCommand cmd = new InfoCommand(ctx);
        cmd.execute();

        assertFalse(messages.isEmpty());
        assertTrue(messages.get(0).contains("0 дн."));
    }

    @Test
    void execute_sameDateLastProlongAndValid_shouldNotCrash() {
        LocalDateTime now = LocalDateTime.now();
        PlayerEntity entity = new PlayerEntity(1L, UUID.randomUUID(), "EdgeCase",
                now, now, true);
        PlayerDto dto = PlayerDto.builder()
                .name("EdgeCase")
                .uuid(entity.getUuid())
                .validUntil(entity.getValidUntil())
                .lastProlongDate(entity.getLastProlongDate())
                .build();

        when(repo.findByName("EdgeCase")).thenReturn(Optional.of(entity));
        when(mapper.entityToDto(entity)).thenReturn(dto);

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{"EdgeCase"})
                .playerRepository(repo)
                .playerMapper(mapper)
                .build();

        InfoCommand cmd = new InfoCommand(ctx);
        assertDoesNotThrow(cmd::execute);

        assertFalse(messages.isEmpty());
        assertTrue(messages.get(0).contains("0%"));
    }

    @Test
    void execute_validPassWith50Percent_shouldDisplayCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastProlong = now.minusDays(15);
        LocalDateTime validUntil = now.plusDays(15);
        PlayerEntity entity = new PlayerEntity(1L, UUID.randomUUID(), "HalfWay",
                validUntil, lastProlong, true);
        PlayerDto dto = PlayerDto.builder()
                .name("HalfWay")
                .uuid(entity.getUuid())
                .validUntil(validUntil)
                .lastProlongDate(lastProlong)
                .build();

        when(repo.findByName("HalfWay")).thenReturn(Optional.of(entity));
        when(mapper.entityToDto(entity)).thenReturn(dto);

        BuildContext ctx = BuildContext.builder()
                .sender(sender)
                .args(new String[]{"HalfWay"})
                .playerRepository(repo)
                .playerMapper(mapper)
                .build();

        InfoCommand cmd = new InfoCommand(ctx);
        cmd.execute();

        assertFalse(messages.isEmpty());
        System.out.println(messages.get(0));
//        assertTrue(messages.get(0).contains("15 дн."));
        assertTrue(messages.get(0).contains("50%"));
    }
}
