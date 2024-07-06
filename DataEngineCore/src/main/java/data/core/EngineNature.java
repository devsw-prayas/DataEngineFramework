package data.core;

import java.lang.annotation.*;

import data.constants.EngineBehaviour;
import data.constants.Ordering;
import data.constants.Nature;

/**
 * This is a marker annotation only. Determines the nature of the engine,
 * whether mutable by nature or are thread-mutable, i.e. can be used
 * in an asynchronous context, or in cases, fully immutable
 * @author Devsw
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface EngineNature {
    Nature nature();
    EngineBehaviour behaviour();
    Ordering order();
}

