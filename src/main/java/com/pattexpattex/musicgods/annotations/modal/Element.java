package com.pattexpattex.musicgods.annotations.modal;

import com.pattexpattex.musicgods.interfaces.modal.objects.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

import java.lang.annotation.*;

@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Element {
    String label();
    String prefill() default Modal.DEF_VALUE;
    String placeholder() default Modal.DEF_PLACEHOLDER;
    boolean required() default Modal.DEF_REQUIRED;
    TextInputStyle style() default TextInputStyle.SHORT;
}
