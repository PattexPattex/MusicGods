package com.pattexpattex.musicgods.annotations;

import net.dv8tion.jda.api.Permission;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Permissions {
    Permission[] value() default {  };
    Permission[] command() default {  };
    Permission[] self() default { Permission.MESSAGE_SEND };

    Permission[] DEFAULT = {  };
    Permission[] SELF_DEFAULT = { Permission.MESSAGE_SEND };
}

