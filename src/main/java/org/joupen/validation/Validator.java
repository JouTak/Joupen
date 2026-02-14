package org.joupen.validation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Упрощённый валидатор команд с автоматической генерацией usage.
 * 
 * Пример:
 * <pre>
 * Validator.of(ctx)
 *     .permission("joupen.admin")
 *     .usage("/joupen prolong <player|all> [duration]")
 *     .arg(0, "player name")
 *     .optionalDuration(1, "3d")
 *     .check();
 * </pre>
 */
public final class Validator {
    private final BuildContext ctx;
    private final CommandSender sender;
    private final String[] args;
    private final List<Component> errors = new ArrayList<>();
    private String usageTemplate;

    private Validator(BuildContext ctx, String[] args) {
        this.ctx = ctx;
        this.sender = ctx.getSender();
        this.args = args;
    }

    public static Validator of(BuildContext ctx, String[] args) {
        return new Validator(ctx, args);
    }

    public Validator permission(String perm) {
        if (sender == null || !sender.hasPermission(perm)) {
            errors.add(Component.text("You don't have permission: " + perm, NamedTextColor.RED));
        }
        return this;
    }

    public Validator usage(String template) {
        this.usageTemplate = template;
        return this;
    }

    public Validator arg(int index, String description) {
        if (args.length <= index) {
            addUsageError("Missing argument: " + description);
        } else if (args[index] == null || args[index].isBlank()) {
            addUsageError("Empty argument: " + description);
        }
        return this;
    }

    public Validator intArg(int index, String description) {
        arg(index, description);
        if (args.length > index && args[index] != null) {
            try {
                Integer.parseInt(args[index]);
            } catch (NumberFormatException e) {
                errors.add(Component.text("Invalid integer for " + description + ": " + args[index], NamedTextColor.RED));
            }
        }
        return this;
    }

    public Validator durationArg(int index, String description) {
        arg(index, description);
        if (args.length > index && args[index] != null) {
            try {
                TimeUtils.parseDuration(args[index]);
            } catch (Exception e) {
                errors.add(Component.text("Invalid duration for " + description + ". Example: 3d2h7m", NamedTextColor.RED));
            }
        }
        return this;
    }

    public Validator optionalDuration(int index, String exampleValue) {
        if (args.length > index && args[index] != null && !args[index].isBlank()) {
            try {
                TimeUtils.parseDuration(args[index]);
            } catch (Exception e) {
                errors.add(Component.text("Invalid duration format. Example: " + exampleValue, NamedTextColor.RED));
            }
        }
        return this;
    }

    public Validator require(boolean condition, String errorMessage) {
        if (!condition) {
            errors.add(Component.text(errorMessage, NamedTextColor.RED));
        }
        return this;
    }

    public List<Component> check() {
        return errors;
    }

    private void addUsageError(String detail) {
        if (usageTemplate != null) {
            errors.add(Component.text("Usage: " + usageTemplate, NamedTextColor.RED));
        } else {
            errors.add(Component.text(detail, NamedTextColor.RED));
        }
    }
}
