package com.pattexpattex.musicgods.annotations;

import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ButtonHandle {
    String identifier();
    String label() default "";

    /**
     * A unicode emoji codepoint ({@code {@literal \}uXXXX}).
     */
    String emoji() default "";
    String url() default "";
    ButtonStyle style() default ButtonStyle.SECONDARY;
}
