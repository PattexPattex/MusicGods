package com.pattexpattex.musicgods.annotations.slash;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface GuildOnly {
}
