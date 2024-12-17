package engine.abstraction;

import data.constants.ImplementationType;
import data.core.AbstractDataEngine;
import data.core.Implementation;
import data.function.UnaryOperator;

/**
 * Top level superclass for all single threaded and multithreaded queue implementations.
 * Defines methods that generalize to all queues. Methods that are specific to blocking queues and
 * multithreaded implementations will be provided via subclassed abstractions
 *
 * @param <E> Type argument of elements being stored
 */
@Implementation(ImplementationType.ABSTRACTION)
public abstract class AbstractQueue<E> extends AbstractDataEngine<E> {

    public AbstractQueue(){
        this(DEFAULT_CAPACITY);
    }

    public AbstractQueue(int capacity){
        super(capacity);
    }

    /**
     * Ensures that the queue is limited to the given {@code maxCapacity} and does not exceed it.
     * @param maxCapacity The max possible capacity to be ensured
     */
    public abstract void ensureCapacity(int maxCapacity);

    /**
     * Adds an element to the queue
     * @param element Element to be added
     * @return Returns true if addition is successful, false otherwise
     */
    public abstract boolean add(E element);

    /**
     * Removes an element from the queue
     * @return Returns the element removed
     */
    public abstract E remove();

    /**
     * Retrieves the element at the head of the queue without removing it
     * @return Returns the element at the head of the queue
     */
    public abstract E peek();

    /**
     * Adds all the elements from {@code queue} to the invoking queue
     * @param queue The queue to be added
     * @param <T> Type of the queue
     */
    public abstract <T extends AbstractQueue<E>> void addAll(T queue);

    /**
     * Checks if the invoking queue contains the given {@code element}
     * @param element Element to be checked for
     * @return Returns true if the element is present, false otherwise
     */
    public abstract boolean contains(E element);

    /**
     * Checks if the invoking queue contains all the elements of {@code queue}
     * @param queue The queue to be checked for
     * @return Returns true if all elements are present, false otherwise
     * @param <T> Type of the queue
     */
    public abstract <T extends AbstractQueue<E>> boolean containsAll(T queue);

    /**
     * Merge the invoking queue with the given {@code queue} and returns a new queue containing all the
     * elements of both the queues
     * @param queue The queue to be merged with
     * @return Returns a new queue containing all the elements of both the queues
     * @param <T> Type of the queue
     */
    public abstract <T extends AbstractQueue<E>> T merge(T queue);

    /**
     * Replaces all the items of the invoking queue with the result of applying the given {@code operator}
     * @param operator The operator to be applied
     */
    public abstract void replaceAll(UnaryOperator<E> operator);
}
