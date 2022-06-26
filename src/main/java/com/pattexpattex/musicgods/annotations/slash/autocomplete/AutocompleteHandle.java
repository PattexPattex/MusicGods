package com.pattexpattex.musicgods.annotations.slash.autocomplete;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutocompleteHandle {
    String value();
}
