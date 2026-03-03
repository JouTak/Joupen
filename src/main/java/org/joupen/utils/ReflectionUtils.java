package org.joupen.utils;

import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandAlias;
import org.joupen.commands.GameCommand;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ReflectionUtils {

    // public static Set<Class<? extends MyInterface>>
    // findClassesImplementsInterface(String packageName) {
    //
    // }

    private static final Map<String, Class<? extends GameCommand>> COMMAND_CACHE = new HashMap<>();

    public static void init() {
        if (!COMMAND_CACHE.isEmpty())
            return;
        Reflections reflections = new Reflections("org.joupen.commands.impl");
        Set<Class<? extends GameCommand>> classes = reflections.getSubTypesOf(GameCommand.class);
        for (Class<? extends GameCommand> clazz : classes) {
            CommandAlias ann = clazz.getAnnotation(CommandAlias.class);
            if (ann != null) {
                COMMAND_CACHE.put(ann.name().toLowerCase(), clazz);
            }
        }
    }

    public static Class<? extends GameCommand> findCommandByAlias(String alias) {
        Class<? extends GameCommand> clazz = COMMAND_CACHE.get(alias.toLowerCase());
        if (clazz == null) {
            throw new IllegalArgumentException("Неизвестная команда");
        }
        return clazz;
    }

    public static Constructor<? extends GameCommand> findCommandConstructorByAlias(String alias) {
        return getCommandConstructor(findCommandByAlias(alias));
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
