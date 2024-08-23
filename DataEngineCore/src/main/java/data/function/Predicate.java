package data.function;

/**
 * Represents an operation that takes in a single operand and returns the result as a boolean
 * This is a {@link  FunctionalInterface} and is a part of {@code DataEngineFramework}
 * @param <T> Type argument of operand
 *
 * @author devsw
 */
public interface Predicate<T> {
    boolean test(T t);
}
