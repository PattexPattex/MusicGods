package com.pattexpattex.musicgods.annotations.modal;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModalHandle {
    String id();
    String title();
}
