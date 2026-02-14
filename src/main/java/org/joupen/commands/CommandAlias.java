package org.joupen.commands;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandAlias {
    String name();

    int minArgs() default 0;

    int maxArgs() default Integer.MAX_VALUE;

    String usage() default "";

    String permission() default "";
}
