package com.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RequestParam {
    String value();                    // Nom du paramètre dans la requête (clé pour getParameter)
    boolean required() default true;   // Si true, erreur si absent (facultatif pour le sprint)
    String defaultValue() default "";  // Valeur par défaut si absent et non required
}