package org.joupen.commands;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.joupen.domain.OperationException;
import org.joupen.domain.OperationResult;
import org.joupen.domain.OperationType;
import org.joupen.domain.PlayerEntity;
import org.joupen.domain.PlayerOperation;
import org.joupen.messaging.Messaging;
import org.joupen.service.PlayerService;
import org.joupen.utils.OperationMessages;
import org.joupen.utils.TimeUtils;

import java.time.DateTimeException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class OperationCommand implements GameCommand {
    protected final CommandSender sender;
    protected final PlayerService playerService;
    private final String[] args;

    protected OperationCommand(BuildContext context) {
        sender = context.getSender();
        playerService = context.getPlayerService();
        args = context.getArgs();
    }

    @Override
    public final void execute() {
        try {
            run(OperationArguments.parse(args));
        } catch (OperationException e) {
            reply(e.getMessage(), Map.of());
        } catch (IllegalArgumentException | DateTimeException | ArithmeticException e) {
            reply("invalid-arguments", Map.of());
        } catch (RuntimeException e) {
            log.error("Operation failed for {}", sender.getName(), e);
            reply("operation-failed", Map.of());
        }
    }

    protected abstract void run(OperationArguments args);

    protected void reply(String key, Map<String, ?> values) {
        Messaging.reply(sender, Component.text(OperationMessages.format(key, values)));
    }

    protected void replyResult(OperationResult result) {
        String key = !result.applied() ? "operation-duplicate"
                : result.operation().getType() == OperationType.UNDO ? "undo-applied" : "operation-applied";
        replyOperation(key, result.operation());
    }

    protected void replyOperation(String key, PlayerOperation operation) {
        PlayerEntity before = operation.getBefore();
        PlayerEntity after = operation.getAfter();
        Map<String, Object> values = new HashMap<>();
        values.put("id", operation.getId());
        values.put("type", operation.getType());
        values.put("date", operation.getOccurredAt());
        long seconds = operation.getDurationSeconds();
        values.put("duration", seconds % 60 == 0 ? TimeUtils.formatDuration(Duration.ofSeconds(seconds)) : seconds + "s");
        values.put("before", display(before == null ? null : before.getValidUntil()));
        values.put("after", display(after.getValidUntil()));
        values.put("approvedBefore", before != null && Boolean.TRUE.equals(before.getApproved()));
        values.put("approvedAfter", Boolean.TRUE.equals(after.getApproved()));
        values.put("temporaryBefore", window(before));
        values.put("temporaryAfter", window(after));
        values.put("reason", display(operation.getMetadata().reason()));
        values.put("source", operation.getMetadata().source());
        values.put("initiator", display(operation.getMetadata().initiator()));
        values.put("externalId", display(operation.getMetadata().externalId()));
        values.put("reverses", display(operation.getReversedOperationId()));
        reply(key, values);
    }

    private String window(PlayerEntity player) {
        return player == null || player.getTemporaryAccessUntil() == null ? "-"
                : player.getTemporaryAccessFrom() + ".." + player.getTemporaryAccessUntil();
    }

    private String display(Object value) {
        return value == null ? "-" : value.toString();
    }
}
