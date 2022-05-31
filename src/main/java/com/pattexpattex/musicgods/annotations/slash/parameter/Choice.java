package com.pattexpattex.musicgods.annotations.slash.parameter;

import java.lang.annotation.*;

@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Choice {
    String[] choices();
    String[] values() default {  };
}
