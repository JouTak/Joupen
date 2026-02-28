package org.joupen.commands;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.joupen.commands.impl.HelpCommand;
import org.joupen.messaging.Messaging;
import org.joupen.utils.ReflectionUtils;
import org.joupen.validation.CommandValidator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
public class JoupenCommandFactory {

    public GameCommand build(BuildContext ctx) {
        String[] commandNameAndArgs = ctx.getCommandArgsWithName();
        log.info("Executing /{} by {} with commandNameAndArgs: {}", ctx.getLabel(), ctx.getSender().getName(), Arrays.toString(commandNameAndArgs));

        if (commandNameAndArgs.length == 0) {
            return new HelpCommand(ctx);
        }

        String commandName = commandNameAndArgs[0].toLowerCase(Locale.ROOT);
        String[] args = Arrays.copyOfRange(commandNameAndArgs, 1, commandNameAndArgs.length);

        Class<? extends GameCommand> commandClass;
        try {
            commandClass = ReflectionUtils.findCommandByAlias(commandName);
        } catch (IllegalArgumentException e) {
            return () -> Messaging.reply(ctx.getSender(),
                    Component.text("Unknown subcommand. Try /joupen help", NamedTextColor.RED));
        }

        CommandAlias alias = commandClass.getAnnotation(CommandAlias.class);

        if (!alias.permission().isEmpty() && !ctx.getSender().hasPermission(alias.permission())) {
            return () -> Messaging.reply(ctx.getSender(),
                    Component.text("You don't have permission: " + alias.permission(), NamedTextColor.RED));
        }

        if (args.length < alias.minArgs() || args.length > alias.maxArgs()) {
            String usage = alias.usage().isEmpty()
                    ? "Expected " + alias.minArgs() + " to " + alias.maxArgs() + " arguments"
                    : "Usage: " + alias.usage();
            return () -> Messaging.reply(ctx.getSender(), Component.text(usage, NamedTextColor.RED));
        }

        BuildContext enrichedCtx = BuildContext.builder()
                .sender(ctx.getSender())
                .label(ctx.getLabel())
                .commandArgsWithName(commandNameAndArgs)
                .args(args)
                .playerRepository(ctx.getPlayerRepository())
                .playerService(ctx.getPlayerService())
                .playerMapper(ctx.getPlayerMapper())
                .transactionManager(ctx.getTransactionManager())
                .build();

        Constructor<? extends GameCommand> constructor = ReflectionUtils.getCommandConstructor(commandClass);
        GameCommand command = null;
        try {
            command = constructor.newInstance(enrichedCtx);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            log.error("Command constructor is not accessible", exception);
        }

        if (command instanceof CommandValidator validator) {
            List<Component> errors = validator.validate(enrichedCtx, args);
            if (!errors.isEmpty()) {
                return () -> errors.forEach(msg -> Messaging.reply(ctx.getSender(), msg));
            }
        }
        return command;
    }
}
