package data.constants;

/**
 * Defines the nature of the data engine
 * @author devsw
 * @since BleedingEdge-alpha-1
 */
public enum Nature {
    /**
     * A mutable data engine is not considered {@code Thread-Safe}
     * instead it is useful for synchronous operations where threads access the object
     * in a synchronous manner
     */
    MUTABLE,

    /**
     * A thread mutable data engine is fully {@code Thread-Safe} and is engineered in
     * a manner as to allow multiple threads to access it in an asynchronous manner
     */
    THREAD_MUTABLE,

    /**
     * An immutable data engine is useful in situations where we only want to
     * use the data for non-destructive operations like outputting.
     */
    IMMUTABLE;

    public String toString(){
        return switch (this){
            case MUTABLE -> "Mutable";
            case THREAD_MUTABLE -> "Thread-Mutable";
            case IMMUTABLE -> "Immutable";
        };
    }
}
