package data.function;

/**
 * Represents an operation that takes two operands and returns a result.
 * This is a {@link FunctionalInterface} and is a part of the {@code DataEngineFramework}
 *
 * @param <T> Type argument of return value
 * @param <U> Type argument of operand
 * @param <R> Type argument of operand
 * @author devsw
 */
public interface BiFunction<T, U, R> {
    R apply(T t, U u);
}
