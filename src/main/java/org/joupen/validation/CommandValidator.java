package org.joupen.validation;

import net.kyori.adventure.text.Component;
import org.joupen.commands.BuildContext;

import java.util.List;

public interface CommandValidator {
    List<Component> validate(BuildContext ctx, String[] args);
}