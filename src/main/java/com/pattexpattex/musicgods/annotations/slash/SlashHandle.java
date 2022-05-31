package com.pattexpattex.musicgods.annotations.slash;

import java.lang.annotation.*;

import static com.pattexpattex.musicgods.interfaces.slash.objects.SlashCommand.DEFAULT_DESCRIPTION;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SlashHandle {
    String path();
    String description() default DEFAULT_DESCRIPTION;
    String baseDescription() default DEFAULT_DESCRIPTION;
}
