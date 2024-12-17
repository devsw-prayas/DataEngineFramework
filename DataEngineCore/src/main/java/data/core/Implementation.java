package data.core;

import data.constants.ImplementationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker annotation, forces the class which is to be defined as a data engine, to
 * extend {@code AbstractDataEngine} or any of its subclasses and provide an {@code Iterator}
 * implementation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Implementation {
    ImplementationType value();
    boolean isSpecialized() default false;
}
