package org.joupen.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.joupen.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public final class CommandValidation {
    private final List<Component> errors = new ArrayList<>();

    private CommandValidation() {
    }

    public static CommandValidation start() {
        return new CommandValidation();
    }

    public CommandValidation require(boolean condition, Component error) {
        if (!condition) {
            errors.add(error);
        }
        return this;
    }

    public CommandValidation requirePermission(CommandSender sender, String permission, Component error) {
        return require(sender != null && sender.hasPermission(permission), error);
    }

    public CommandValidation requireInt(String raw, Component error) {
        if (raw == null || raw.isBlank()) {
            errors.add(error);
            return this;
        }
        try {
            Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            errors.add(error);
        }
        return this;
    }

    public CommandValidation requireDuration(String raw, Component error) {
        if (raw == null || raw.isBlank()) {
            errors.add(error);
            return this;
        }
        try {
            TimeUtils.parseDuration(raw);
        } catch (Exception e) {
            errors.add(error);
        }
        return this;
    }

    public List<Component> errors() {
        return errors;
    }
}
