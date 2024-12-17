package engine.abstraction;

import data.constants.ImplementationType;
import data.core.AbstractDataEngine;
import data.core.Implementation;
import data.function.UnaryOperator;
import data.core.ImmutableException;
import data.core.EngineUnderflowException;

/**
 * The top level superclass for all double-ended queues. A double-ended queue is one that allows
 * addition and removal of items at both ends. This abstraction defines all the behavior common to
 * all possible implementations of a deque. Any concurrent behavior is to be added manually per
 * implementation.
 *
 * @param <E> Type argument of data being stored
 *
 * @author Devsw
 */
@Implementation(value = ImplementationType.ABSTRACTION)
public abstract class AbstractDeque<E> extends AbstractDataEngine<E>{

    public AbstractDeque() {
        super(DEFAULT_CAPACITY);
    }

    public AbstractDeque(int capacity) {
        super(capacity);
    }

    public abstract void ensureCapacity(int minCapacity);

    /**
     * Adds an element to the head of the deque
     * @param element Element to be added
     * @return Returns true if addition occurs, false otherwise
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    public abstract boolean addFirst(E element);

    /**
     * Adds all the elements from {@code deque}  at the head of the invoking deque
     * @param deque The deque to be added to the head
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    public abstract <T extends AbstractDeque<E>> void addFirstAll(T deque);

    /**
     * Adds all the elements from {@code deque} at the tail of the invoking deque
     * @param deque The deque to be added to the tail
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    public abstract <T extends AbstractDeque<E>> void addLastAll(T deque);

    /**
     * Adds the given {@code element} to the end of the invoking deque
     * @param element Element to be added
     * @return Returns true if addition is successful
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    public abstract boolean addLast(E element);

    /**
     * Removes an element from the head of the invoking deque
     * @return Returns the element removed, null otherwise
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    public abstract E removeFirst();

    /**
     * Removes an element from the tail of the invoking deque
     * @return Returns the element removed, null otherwise
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    public abstract E removeLast();

    /**
     * Obtains the element present at the head of the invoking deque
     * @return Returns the element present at the head
     */
    public abstract E peekFirst();

    /**
     * Obtains the element present at the tail of the invoking deque
     * @return Returns the element present at the tail
     */
    public abstract E peekLast();

    /**
     * Checks if {@code element} is present in the invoking deque
     * @param element The element to be checked for
     * @return Returns true if present, false otherwise
     */
    public abstract boolean contains(E element);

    /**
     * Checks if all the elements of {@code deque} is present in the invoking deque.
     * @param deque The deque to be checked for
     * @return Returns true if all elements are present, false otherwise
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    public abstract  <T extends AbstractDeque<E>> boolean containsAll(T deque);


    /**
     * Retains all the elements of {@code deque} that are present in the given deque
     * @param deque The deque to be compared with
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    public abstract <T extends AbstractDeque<E>> void retainAll(T deque);

    /**
     * Merges the given {@code deque} to the tail of the invoking deque
     * @param deque The deque to be merged with
     * @return Returns the merged deque
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    public abstract <T extends AbstractDeque<E>> T mergeLast(T deque);

    /**
     * Merges the given {@code deque} to the head of the invoking deque
     * @param deque The deque to be merged with
     * @return Returns the merged deque
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    public abstract <T extends AbstractDeque<E>> T mergeFirst(T deque);

    /**
     * Modifies and replaces all the elements present in the invoking deque by passing them into
     * an {@link UnaryOperator}
     * @param operator The {@link UnaryOperator} to be used.
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    public abstract void replaceAll(UnaryOperator<E> operator);
}