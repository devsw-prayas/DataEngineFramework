package data.core;

import java.util.concurrent.atomic.AtomicReference;
import data.constants.Type;
import data.constants.Nature;

/**
 * The absolute superclass of the {@code Data Engine} hierarchy
 * Various partial implementations are created via implementing this interface.
 * Contains behaviour common to every data engine. The feature set is conceptually similar to
 * the collections framework defined by the JDK
 *
 * @author Devsw
 * @param <E> Type of elements being stored in a data engine
 *
 * @see java.util.Collection
 * @see Iterable
 * @see EngineNature
 * @see AtomicReference
 * @see EngineUnderflowException
 * @see UnsupportedOperationException
 */
public interface DataEngine<E>{

    /**
     * Many data engines are implemented as mutable, i.e. the data can be manipulated in a
     * destructive manner. For algorithms that will require the mutable behaviour
     * it becomes necessary to check.
     *
     * @return returns true if the data engine is mutable, false otherwise
     */
    @Behaviour(Type.IMMUTABLE)
    default boolean isMutable(){
        EngineNature anno = this.getClass().getAnnotation(EngineNature.class);
        return switch (anno.nature()){
            case MUTABLE, THREAD_MUTABLE -> true;
            case IMMUTABLE -> false;
        };
    }

    /**
     * @return Returns true if the data engine is thread-safe, false otherwise
     */
    @Behaviour(Type.IMMUTABLE)
    default boolean isThreadSafe(){
        EngineNature anno = this.getClass().getAnnotation(EngineNature.class);
        return anno.nature().equals(Nature.THREAD_MUTABLE);
    }

    /**
     * In some cases, it would become convenient to use an array for iterative purposes for
     * faster run times. Thus, it becomes a better alternative to use arrays.
     *
     * @return It returns a deep-copy array view of the entire data engine
     * @throws EngineUnderflowException In case, the data engine is empty
     *  an exception is generated
     */
    E[] toArray() throws EngineUnderflowException;

    /**
     * Similar to the {@code toArray} method, it creates an array from all objects from {@code start}
     * inclusive, and ends at the final element. Can throw {@code IndexOutOfBoundsException} when invalid
     * index is passed
     *
     * @param start The starting position for extraction
     * @return Returns an array containing the required elements
     * @throws EngineUnderflowException Thrown when invoking data engine is empty
     */
    @Behaviour(Type.UNSUPPORTED)
    default E[] rangedToArray(int start) throws EngineUnderflowException{
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     * Similar to the {@code toArray} method, it creates an array from all object from {@code start} to {@code end}
     * inclusive. Can throw {@code IndexOutOfBoundsException} when invalid index is passed
     *
     * @param start The starting point for extraction
     * @param end The end point of extraction
     * @return Returns an array containing the required elements
     * @throws EngineUnderflowException Thrown when invoking data engine is empty
     */
    @Behaviour(Type.UNSUPPORTED)
    default E[] rangedToArray(int start, int end) throws EngineUnderflowException{
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     * @return Returns current size of data engine
     */
    int getActiveSize();

    /**
     * @return Returns max capacity of the data engine, or the max size before growth
     */
    int getMaxCapacity();

    /**
     * @return Returns true, if the invoking data engine is empty, false otherwise
     */
    boolean isEmpty();

    /**
     * @return Returns true if the invoking data engine has been emptied, false otherwise
     * @throws EngineUnderflowException If the data engine is empty, throws an exception
     */
    boolean removeAll() throws EngineUnderflowException, ImmutableException;

    /**
     * Generates an {@code AtomicReference} of the data engine. Useful when the object has to be
     * used in a thread blocking context without making the data engine asynchronous.
     * @return Returns an {@code AtomicReference} of the invoking data engine
     * @param <T> A concrete implementation of {@code DataEngine}
     * @throws UnsupportedOperationException Will throw an exception when the invoking data engine is
     * already {@code Thread-Mutable}
     */
    <T extends DataEngine<E>> AtomicReference<T> createThreadSafeImage();

    /**
     * Checks if the invoking data engine and the data engine passed are truly equal, i.e. positions of all elements
     * are identical
     *
     * @param de The data engine to be compared
     * @return Returns true if both are equals, false otherwise
     * @param <T> Subclass of {@code DataEngine}
     * @throws EngineUnderflowException Thrown when either of them is empty, or if both are of
     * different lengths
     */
    @Behaviour(Type.UNSUPPORTED)
    default <T extends DataEngine<E>> boolean equals(T de) throws EngineUnderflowException {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    /**
     * Checks within an exclusive-bounded range the equality of the given data engine and the invoking
     * data engine. Behaviour similar to {@code equals}
     *
     * @param de The data engine to be compared with me
     * @param start The starting position, exclusive of range
     * @param end The ending position, exclusive of range
     * @return Returns true if the range are equal for both
     * @param <T> Subclass of {@code DataEngine}
     * @throws EngineUnderflowException Thrown when either of them is empty, or range length is invalid
     */
    @Behaviour(Type.UNSUPPORTED)
    default <T extends DataEngine<E>> boolean rangeEquals(T de, int start, int end) throws EngineUnderflowException {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    /**
     * Checks if both data engines contain the same elements, irrespective of repetitions
     *
     * @param de The data engine to be compared with
     * @return Returns true if both are equivalent, false otherwise
     * @param <T> Subclass of {@code DataEngine}
     */
    @Behaviour(Type.UNSUPPORTED)
    default <T extends DataEngine<E>> boolean equivalence(T de) throws EngineUnderflowException {
        throw new UnsupportedOperationException("Unsupported operation");
    }
}
