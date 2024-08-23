package data.function;

/**
 * Represents an operation that takes in a single operand and returns the result
 * This ia a {@link FunctionalInterface} and is a part of {@code DataEngineFramework}
 * @param <T> Type argument of operand
 * @param <R> Type argument of return value
 *
 * @author devsw
 */
public interface Function<T, R> {
    R apply(T t);
}
