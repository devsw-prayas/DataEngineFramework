package data.core;

import java.io.Serial;

/**
 * Defines the immutable exception which will be thrown when mutable
 * methods are called on an immutable data engine
 * @author DevSW
 */
public class ImmutableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 328004990876528964L;

    public ImmutableException() {
        super("Immutable Data Engine");
    }

    public ImmutableException(String message) {
        super(message);
    }
}
