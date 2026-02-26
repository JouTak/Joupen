package org.joupen.utils;

import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandAlias;
import org.joupen.commands.GameCommand;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Set;

public class ReflectionUtils {

//    public static Set<Class<? extends MyInterface>> findClassesImplementsInterface(String packageName) {
//
//    }

    public static Set<Class<? extends GameCommand>> findClassesImplementsInterfaceGameCommand() {
        Reflections reflections = new Reflections("org.joupen.commands.impl");
        Set<Class<? extends GameCommand>> classes = reflections.getSubTypesOf(GameCommand.class);
        return classes;
    }

    public static Class<? extends GameCommand> findCommandByAlias(String alias) {
        return findClassesImplementsInterfaceGameCommand().stream()
                .filter(c -> {
                    CommandAlias ann = c.getAnnotation(CommandAlias.class);
                    return ann != null && ann.name().equalsIgnoreCase(alias);
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Неизвестная команда"));
    }

    public static Constructor<? extends GameCommand> findCommandConstructorByAlias(String alias) {
        Class<? extends GameCommand> commandClass = findClassesImplementsInterfaceGameCommand().stream()
                .filter(c -> {
                    CommandAlias ann = c.getAnnotation(CommandAlias.class);
                    return ann != null && ann.name().equalsIgnoreCase(alias);
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Неизвестная команда"));

        return getCommandConstructor(commandClass);
    }

    public static Constructor<? extends GameCommand> getCommandConstructor(Class<? extends GameCommand> clazz) {
        try {
            return clazz.getConstructor(BuildContext.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "У команды " + clazz.getName() + " нет конструктора (BuildContext)", e);

        }
    }

}
