package org.joupen.commands;

import net.kyori.adventure.text.Component;

import java.util.List;

public interface CommandValidator {
    List<Component> validate(BuildContext ctx, String[] args);
}