package com.pattexpattex.musicgods.annotations.slash;

import com.pattexpattex.musicgods.interfaces.slash.objects.SlashGroup;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Grouped {
    String value();
    String name() default "";
    boolean hidden() default false;
    String description() default SlashGroup.DEFAULT_DESCRIPTION;
    String emoji() default "";
}
