package com.pattexpattex.musicgods.annotations.selection;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SelectionHandle {
    String value();
}
