package com.pattexpattex.musicgods.annotations.slash.parameter;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Parameter {
    String name() default "";
    String description() default "No description.";
    boolean required() default true;
}
