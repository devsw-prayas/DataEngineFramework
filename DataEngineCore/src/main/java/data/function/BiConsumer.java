package data.function;

/**
 * Represents an operation that accepts two operands.
 * This is a {@link FunctionalInterface} and is a part of the {@code DataEngineFramework}
 * @param <T> Type argument of operand
 * @param <U> Type argument of operand
 *
 * @author devsw
 */
public interface BiConsumer<T, U> {
    void accept(T t, U u);
}
