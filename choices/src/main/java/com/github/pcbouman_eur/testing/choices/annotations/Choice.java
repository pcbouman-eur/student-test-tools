package com.github.pcbouman_eur.testing.choices.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Choice {
    String name();
    double points() default 1.0;
    TestStep[] steps();
}
