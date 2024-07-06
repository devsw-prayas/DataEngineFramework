package data.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import data.constants.Type;

/**
 * A special maker annotation that can be marked on methods that are either part of an {@code IMPLEMENTATION}
 * or {@code ABSTRACTION}, when marked as {@code MUTABLE} the processor will replace the methods with versions that
 * throw an {@code UnsupportedOperationException}. This can be only used inside classes that are labelled as
 * {@code Implementation}. It is necessary that the required methods belong to a class hierarchy, as it will pull methods
 * from the superclass and provide redefinition. To use it efficiently, simply mark the method as {@code IMMUTABLE}
 * or {@code MUTABLE}, and while defining a subclass leave behind the methods marked as {@code MUTABLE} to let the
 * processor generate the blocking methods. This only applies to class that belong to the Data-Engine framework.
 * Any class that is considered an immutable implementation of a mutable engine is viable candidate for
 * this annotation. Methods which already are unsupported but are overridden to simply throw an exception must be
 * marked as {@code UNSUPPORTED}
 * <p>
 * Thus, the following conditions need to be satisfied.
 * <p> {@code 1} The class must be a subclass of a concrete implementation marked as {@code MUTABLE}
 * <p> {@code 2} The methods that are mutable, must not be overridden.
 * <p> {@code 3} The method cannot be abstract
 *
 * @author Devsw
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Behaviour {
    Type value();
}
