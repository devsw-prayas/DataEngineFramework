package data.function;

/**
 * Represents an operation that takes a single operand.
 * This is a {@link FunctionalInterface} and is part of the {@code DataEngineFramework}
 * @param <T> Type argument of operand
 *
 * @author devsw
 */
public interface UnaryOperator<T> {
    void perform(T t);
}
