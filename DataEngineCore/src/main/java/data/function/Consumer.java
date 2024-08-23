package data.function;

/**
 * Represents an operation that takes in a single operand and operates on it.
 * This is a {@link FunctionalInterface} and is a part of the {@code DataEngineFramework}
 * @param <T> Type argument of operand
 *
 * @author devsw
 */
public interface Consumer<T>{
    void accept(T t);
}
