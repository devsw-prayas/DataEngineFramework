package data.function;

/**
 * Represents an operation that supplies a value of the given type. It takes no parameters
 * This is a {@link FunctionalInterface} and is a part of  {@code DataEngineFramework}
 * @param <T> Type argument of return value
 *
 * @author devsw
 */
public interface Supplier<T> {
    T get();
}
