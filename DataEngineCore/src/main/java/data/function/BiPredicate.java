package data.function;

/**
 * Represents an operation that takes in two operands and returns the result as a boolean.
 * This is a {@link FunctionalInterface} and is a part of the {@code DataEngineFramework}
 * @param <T> Type argument of operand
 * @param <U> Type argument of operand
 * @author devsw
 */
public interface BiPredicate<T, U> {
    boolean test(T t, U u);
}
