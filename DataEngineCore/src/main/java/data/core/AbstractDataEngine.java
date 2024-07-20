package data.core;


import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import data.constants.ImplementationType;
import data.constants.Type;

/**
 * This class defines methods, that will be common to every single data engine, irrespective of type of
 * data structure, or its implementation
 * @param <E> Type of data being stored
 */
@Implementation(value = ImplementationType.ABSTRACTION)
public abstract class AbstractDataEngine<E> implements DataEngine<E>, Iterable<E>{

    //The golden ratio, generally used as a growth factor for determining new size
    protected final static double GOLDEN_RATIO = 1.61803398875;

    //Only for array-based
    protected final static int DEFAULT_CAPACITY = 16;
    protected final static double GROWTH_LOAD_FACTOR = 0.75;
    protected final static double SHRINK_LOAD_FACTOR = 0.25;

    private volatile int activeSize;
    private volatile int maxCapacity;

    protected AbstractDataEngine(int maxCapacity){
        this.activeSize = 0;
        this.maxCapacity = maxCapacity;
    }

    /**
     * When invoked on a data engine that implements an underlying array, will shift all the elements
     * to the beginning, i.e. a sparsely populated array can be so adjusted that all the elements
     * get move to the front
     *
     */
    protected abstract void compress() throws ImmutableException;

    /**
     * When the {@code activeSize} is less than {@code SHRINK_LOAD_FACTOR * maxCapacity}, for an
     * underlying array it will end up shrinking by {@code Math.floor(GOLDEN_RATIO * maxCapacity)}
     * Cam have an asynchronous implementation in thread-safe data engines.
     *
     */
    protected abstract void shrink() throws ImmutableException;

    /**
     * When the {@code activeSize} is greater than {@code GROWTh_LOAD_FACTOR * maxCapacity}, for an
     * underlying array it will end up growing by {@code Math.floor(GOLDEN_RATIO * maxCapacity)}
     * Can have an asynchronous implementation in thread-safe data engines.
     *
     */
    protected abstract void grow() throws ImmutableException;

    /**
     * @return Returns true, if the invoking data engine is empty, false otherwise
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public boolean isEmpty() {
        return this.activeSize == 0;
    }

    /**
     * @return Returns max capacity of the data engine, or the max size before growth
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    /**
     * Sets the new max capacity of the data-engine
     */
    @Behaviour(Type.IMMUTABLE)
    protected void setMaxCapacity(int maxCapacity){
        this.maxCapacity = maxCapacity;
    }

    /**
     * Sets the new active size of the data-engine.
     */
    @Behaviour(Type.IMMUTABLE)
    protected void setActiveSize(int activeSize){
        this.activeSize = activeSize;
    }

    /**
     * @return Returns current size of data engine
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public int getActiveSize() {
        return this.activeSize;
    }

    /**
     * Generates an {@code AtomicReference} of the data engine. Useful when the object has to be
     * used in a thread blocking context without making the data engine asynchronous.
     *
     * @return Returns an {@code AtomicReference} of the invoking data engine
     * @throws UnsupportedOperationException Will throw an exception when the invoking data engine is
     *         already {@code Thread-Mutable}
     */
    @Override
    @SuppressWarnings("unchecked")
    @Behaviour(Type.IMMUTABLE)
    public <T extends DataEngine<E>> AtomicReference<T> createThreadSafeImage() {
        return new AtomicReference<>((T)this);
    }

    /**
     * The method reverses the invoking data engine when implemented.
     *
     * @throws UnsupportedOperationException Thrown when it is unimplemented.
     */
    @Behaviour(Type.UNSUPPORTED)
    public abstract void reverse() throws ImmutableException;

    //Must be present to prevent invalid code in subclass abstractions.
    @Override
    @Behaviour(Type.UNSUPPORTED)
    public Iterator<E> iterator(){
        throw new UnsupportedOperationException("No possible implementation");
    }


    /**
     * Merges the {@code de} provided with the invoking list. A new de is generated with
     * max capacity equal to sum of max-capacities of both de containing all the elements
     *
     * @param de The provided data-engine with which merging is to take place
     * @return Returns the merged data-engine
     * @param <T> A subclass of {@code DataEngine}
     */
    @Behaviour(Type.UNSUPPORTED)
    public <T extends DataEngine<E>> T merge(T de) throws ImmutableException {
        throw new UnsupportedOperationException("No possible implementation");
    }

    /**
     * Merges the {@code de} provided with the invoking data-engine. Only the items present after
     * start are merged with the invoking data-engine.
     *
     * @param de The provided data-engine with which merging is to take place
     * @param start The start point of extraction
     * @return Returns the merged data-engine
     * @param <T> A subclass of {@code DataEngine}
     */
    @Behaviour(Type.UNSUPPORTED)
    public <T extends DataEngine<E>> T merge(T de, int start) throws ImmutableException {
        throw new UnsupportedOperationException("No possible implementation");
    }

    /**
     * Merges the {@code de} provided with the invoking data-engine. Only the items present in the rang
     * {@code start} to {@code end} (inclusive) are merged with the invoking data-engine.
     *
     * @param de The provided data-engine with which merging is to take place
     * @param start The start point of extraction
     * @param end The end point of extraction
     * @return Returns the merged data-engine
     * @param <T> A subclass of {@code DataEngine}
     */
    @Behaviour(Type.UNSUPPORTED)
    public <T extends DataEngine<E>> T merge(T de, int start, int end) throws ImmutableException {
        throw new UnsupportedOperationException("No possible implementation");
    }
}
