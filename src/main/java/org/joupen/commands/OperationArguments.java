package org.joupen.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.joupen.domain.OperationException;
import org.joupen.domain.OperationMetadata;

import java.util.ArrayList;
import java.util.List;

public record OperationArguments(List<String> values, String source, String externalId) {
    public static OperationArguments parse(String[] args) {
        List<String> values = new ArrayList<>();
        String source = null;
        String externalId = null;
        for (String arg : args) {
            if (arg.startsWith("--source=") && source == null) {
                source = arg.substring("--source=".length());
            } else if (arg.startsWith("--external-id=") && externalId == null) {
                externalId = arg.substring("--external-id=".length());
            } else if (arg.startsWith("--")) {
                throw new OperationException("invalid-arguments");
            } else {
                values.add(arg);
            }
        }
        return new OperationArguments(List.copyOf(values), source, externalId);
    }

    public String get(int index) {
        if (index >= values.size()) throw new OperationException("invalid-arguments");
        return values.get(index);
    }

    public OperationMetadata metadata(CommandSender sender, int reasonIndex) {
        String reason = String.join(" ", values.subList(Math.min(reasonIndex, values.size()), values.size()));
        String origin = source;
        if (origin == null) origin = sender instanceof Player ? "command" : sender instanceof RemoteConsoleCommandSender ? "rcon" : "console";
        return new OperationMetadata(reason.isBlank() ? null : reason, origin, sender.getName(), externalId);
    }
}
