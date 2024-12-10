package engine.abstraction;

import data.constants.ImplementationType;
import data.constants.Type;
import data.core.AbstractDataEngine;
import data.core.Behaviour;
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
    @Behaviour(Type.UNSUPPORTED)
    public abstract boolean addFirst(E element);

    /**
     * Adds all the elements from {@code deque}  at the head of the invoking deque
     * @param deque The deque to be added to the head
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    @Behaviour(Type.UNSUPPORTED)
    public abstract <T extends AbstractDeque<E>> void addFirstAll(T deque);

    /**
     * Adds all the elements from {@code deque} at the tail of the invoking deque
     * @param deque The deque to be added to the tail
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    @Behaviour(Type.UNSUPPORTED)
    public abstract <T extends AbstractDeque<E>> void addLastAll(T deque);

    /**
     * Adds the given {@code element} to the end of the invoking deque
     * @param element Element to be added
     * @return Returns true if addition is successful
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    @Behaviour(Type.UNSUPPORTED)
    public abstract boolean addLast(E element);

    /**
     * Removes an element from the head of the invoking deque
     * @return Returns the element removed, null otherwise
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    @Behaviour(Type.UNSUPPORTED)
    public abstract E removeFirst();

    /**
     * Removes an element from the tail of the invoking deque
     * @return Returns the element removed, null otherwise
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    @Behaviour(Type.UNSUPPORTED)
    public abstract E removeLast();

    /**
     * Obtains the element present at the head of the invoking deque
     * @return Returns the element present at the head
     */
    @Behaviour(Type.UNSUPPORTED)
    public abstract E peekFirst();

    /**
     * Obtains the element present at the tail of the invoking deque
     * @return Returns the element present at the tail
     */
    @Behaviour(Type.UNSUPPORTED)
    public abstract E peekLast();

    /**
     * Checks if {@code element} is present in the invoking deque
     * @param element The element to be checked for
     * @return Returns true if present, false otherwise
     */
    @Behaviour(Type.UNSUPPORTED)
    public abstract boolean contains(E element);

    /**
     * Checks if all the elements of {@code deque} is present in the invoking deque.
     * @param deque The deque to be checked for
     * @return Returns true if all elements are present, false otherwise
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    @Behaviour(Type.IMMUTABLE)
    public <T extends AbstractDeque<E>> boolean containsAll(T deque){
        return containsAll(0, deque.getActiveSize() -1, deque);
    }

    /**
     * Checks if all the elements of {@code deque} lying beyond {@code start} are in the
     * invoking deque.
     * @param start Start index
     * @param deque Deque to be checked for
     * @return Returns true if all elements are present, false otherwise
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    @Behaviour(Type.IMMUTABLE)
    public <T extends AbstractDeque<E>> boolean containsAll(int start, T deque){
        return containsAll(start, deque.getActiveSize()-1, deque);
    }

    /**
     * Retains all the elements of {@code deque} that are present in the given deque
     * @param deque The deque to be compared with
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    @Behaviour(Type.UNSUPPORTED)
    public abstract <T extends AbstractDeque<E>> void retainAll(T deque);

    /**
     * Checks if all the elements present in the given {@code deque} lying between {@code start}
     * and {@code end} are present in the invoking deque.
     * @param start Start Index
     * @param end End Index
     * @param deque The deque to be checked for
     * @return Returns true if all the elements are present, false otherwise
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    @Behaviour(Type.UNSUPPORTED)
    public abstract <T extends AbstractDeque<E>> boolean containsAll(int start, int end, T deque);

    /**
     * Merges the given {@code deque} to the tail of the invoking deque
     * @param deque The deque to be merged with
     * @return Returns the merged deque
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    @Behaviour(Type.UNSUPPORTED)
    public abstract <T extends AbstractDeque<E>> T mergeLast(T deque);

    /**
     * Merges the given {@code deque} to the head of the invoking deque
     * @param deque The deque to be merged with
     * @return Returns the merged deque
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    @Behaviour(Type.UNSUPPORTED)
    public abstract <T extends AbstractDeque<E>> T mergeFirst(T deque);

    /**
     * Modifies and replaces all the elements present in the invoking deque by passing them into
     * an {@link UnaryOperator}
     * @param operator The {@link UnaryOperator} to be used.
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    @Behaviour(Type.MUTABLE)
    public void replaceAll(UnaryOperator<E> operator) {
        replaceAll(operator, 0, getActiveSize()-1);
    }

    /**
     * Modifies and replaces all the elements present in the invoking deque lying in the range
     * {@code start} and {@code end} by passing through an {@link UnaryOperator}
     * @param operator The {@link UnaryOperator} to be used
     * @param start Start index
     * @param end End index
     * @throws ImmutableException Thrown when the invoking deque is immutable
     * @throws EngineUnderflowException Thrown when the {@code deque} is empty.
     */
    @Behaviour(Type.UNSUPPORTED)
    public abstract void replaceAll(UnaryOperator<E> operator, int start, int end);
}