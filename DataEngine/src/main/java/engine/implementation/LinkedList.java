package engine.implementation;

import data.constants.*;
import data.core.*;
import data.function.UnaryOperator;
import engine.abstraction.AbstractList;

import java.util.Objects;

/**
 * A highly-efficient implementation of a doubly-linked list. A doubly-linked list contains nodes which
 * point both to the next and previous node. A doubly-linked list is considered a good option when lists of
 * data are required but no info about how much data will be stored is provided at compile time. Although
 * extremely fast, they lack speed during access and retrieval when compared to other implementations
 * like {@link DynamicArrayList} or even {@link ConcurrentArrayList}. In terms of memory footprint,
 * the doubly-linked list would excel as each node contains a pointer to an item, and two pointers for the next
 * and previous nodes, compared to the aforementioned implementations which grow in a controlled manner
 * often leaving behind unused space. This implementation provides a {@link data.core.ListIterator} that can
 * iterate bidirectionally on the list. It is one of those classes that implements {@link Sortable}
 * as it provides a custom sorting algorithm internally.
 *
 * @param <E> Type argument of data
 */
@Implementation(ImplementationType.IMPLEMENTATION)
@EngineNature(behaviour = EngineBehaviour.DYNAMIC, nature = Nature.MUTABLE, order = Ordering.UNSORTED)
public class LinkedList<E> extends AbstractList<E> implements Sortable {

    public LinkedList() {
        super(0);
        head = new LinkedNode<>();
        tail = head;
    }

    //Head and tail nodes for linked list
    private LinkedNode<E> head;
    private LinkedNode<E> tail;

    /**
     * Increment the size by 1
     */
    @Behaviour(Type.IMMUTABLE)
    private void incrementSize(){
        setActiveSize(getActiveSize() + 1);
    }

    /**
     * Decrement the size by 1
     */
    @Behaviour(Type.IMMUTABLE)
    private void decrementSize(){
        setActiveSize(getActiveSize() - 1);
    }

    /**
     * Sets the item at given {@code idx} (assuming it to be valid) to the given {@code index}
     *
     * @param idx  The position where the item is to be changed
     * @param item The item
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void set(int idx, E item) {

    }

    @Override
    @Behaviour(Type.MUTABLE)
    public void add(E item) {

    }

    @Override
    @Behaviour(Type.MUTABLE)
    public void add(int index, E item) {

    }

    /**
     * Adds all items present in {@code arr} in the range {@code start} to {@code end} inclusive. All the items
     * present must be non-null, or an exception will be thrown. It depends on the {@code add} method. For thread-safe
     * implementations, a more efficient implementation is preferred
     *
     * @param arr   An array containing non-null items to add
     * @param start Start point for adding elements
     * @param end   End point for adding elements
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void addAll(E[] arr, int start, int end) {

    }

    /**
     * Checks if an item is present in the invoking list
     *
     * @param item The item to be checked
     * @return Returns true if present, false otherwise
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public boolean contains(E item) {
        return false;
    }

    /**
     * Removes the {@code item} if it is present in the list. All possible occurrences are removed
     *
     * @param item The item to bo removed
     * @return Returns true if removed, false otherwise.
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public boolean remove(E item) {
        return false;
    }

    /**
     * Removes the item present at the given index as long as it is not null
     *
     * @param index The position at which an item (if present) is to be removed
     * @return Returns true if an item is removed, false otherwise
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public boolean removeAt(int index) {
        return false;
    }

    /**
     * Clears all the items in the list.
     *
     * @return Returns true if cleared, false otherwise.
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public boolean clear() {
        return false;
    }

    /**
     * Finds the first index of the passed item in the invoking list. Will return -1 if the
     * item is not present in the list
     *
     * @param item The item whose first position is to be calculated
     * @return Returns the index position if present, else -1
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public int getFirstIndexOf(E item) {
        return 0;
    }

    /**
     * Finds the last index of the passed item in the invoking list. Will return -1 if the
     * item is not present in the list
     *
     * @param item The item whose last position is to be calculated
     * @return Returns the index if present, else -1
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public int getLastIndexOf(E item) {
        return 0;
    }

    /**
     * Returns the item present at the given {@code index}, if valid.
     *
     * @param index The position of retrieval of item
     * @return Returns the item present at the given index.
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public E get(int index) {
        return null;
    }

    /**
     * Creates a list containing all the elements in the range {@code start} to {@code end}.
     * Null indices are not allowed
     *
     * @param start Starting position
     * @param end   End position
     * @return Returns the new list
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public AbstractList<E> subList(int start, int end) {
        return null;
    }

    /**
     * Performs the operation defined by {@code operator} on all the items lying in the range {@code start}
     * to {@code end}. The actual elements in the underlying array or structure are directly modified
     *
     * @param operator An {@link UnaryOperator} that is applied on all the elements present in the range
     * @param start    Starting index
     * @param end      End index
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void replaceAll(UnaryOperator<E> operator, int start, int end) {

    }

    /**
     * When invoked on a data engine that implements an underlying array, will shift all the elements
     * to the beginning, i.e. a sparsely populated array can be so adjusted that all the elements
     * get move to the front
     */
    @Override
    @Behaviour(Type.UNSUPPORTED)
    protected void compress() {
        throw new UnsupportedOperationException("Operation not supported");
    }

    /**
     * When the {@code activeSize} is less than {@code SHRINK_LOAD_FACTOR * maxCapacity}, for an
     * underlying array it will end up shrinking by {@code Math.floor(GOLDEN_RATIO * maxCapacity)}
     * Cam have an asynchronous implementation in thread-safe data engines.
     */
    @Override
    @Behaviour(Type.UNSUPPORTED)
    protected void shrink(){
        throw new UnsupportedOperationException("Operation not supported");
    }

    /**
     * When the {@code activeSize} is greater than {@code GROWTh_LOAD_FACTOR * maxCapacity}, for an
     * underlying array it will end up growing by {@code Math.floor(GOLDEN_RATIO * maxCapacity)}
     * Can have an asynchronous implementation in thread-safe data engines.
     */
    @Override
    @Behaviour(Type.UNSUPPORTED)
    protected void grow() {
        throw new UnsupportedOperationException("Operation not supported");
    }

    /**
     * The method reverses the invoking data engine when implemented.
     *
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void reverse() {

    }

    /**
     * In some cases, it would become convenient to use an array for iterative purposes for
     * faster run times. Thus, it becomes a better alternative to use arrays.
     *
     * @return It returns a deep-copy array view of the entire data engine
     * @throws EngineUnderflowException In case, the data engine is empty
     *                                  an exception is generated
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public E[] toArray() {
        return null;
    }

    /**
     * Similar to the {@code toArray} method, it creates an array from all objects from {@code start}
     * inclusive, and ends at the final element. Can throw {@code IndexOutOfBoundsException} when invalid
     * index is passed
     *
     * @param start The starting position for extraction
     * @return Returns an array containing the required elements
     * @throws EngineUnderflowException Thrown when invoking data engine is empty
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public E[] toArray(int start) {
        return super.toArray(start);
    }

    /**
     * Similar to the {@code toArray} method, it creates an array from all object from {@code start} to {@code end}
     * inclusive. Can throw {@code IndexOutOfBoundsException} when invalid index is passed
     *
     * @param start The starting point for extraction
     * @param end   The end point of extraction
     * @return Returns an array containing the required elements
     * @throws EngineUnderflowException Thrown when invoking data engine is empty
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public E[] toArray(int start, int end) {
        return super.toArray(start, end);
    }

    /**
     * @return Returns true if the invoking data engine has been emptied, false otherwise
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public boolean removeAll() {
        return false;
    }

    /**
     * Checks if the invoking data engine and the data engine passed are truly equal, i.e. positions of all elements
     * are identical
     *
     * @param de The data engine to be compared
     * @return Returns true if both are equals, false otherwise
     * @throws EngineUnderflowException Thrown when either of them is empty, or if both are of
     *                                  different lengths
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public <T extends DataEngine<E>> boolean equals(T de) {
        return super.equals(de);
    }

    /**
     * Checks within an exclusive-bounded range the equality of the given data engine and the invoking
     * data engine. Behaviour similar to {@code equals}
     *
     * @param de    The data engine to be compared with me
     * @param start The starting position, exclusive of range
     * @param end   The ending position, exclusive of range
     * @return Returns true if the range are equal for both
     * @throws EngineUnderflowException Thrown when either of them is empty, or range length is invalid
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public <T extends DataEngine<E>> boolean equals(T de, int start, int end) {
        return super.equals(de, start, end);
    }

    /**
     * Checks if both data engines contain the same elements, irrespective of repetitions
     *
     * @param de The data engine to be compared with
     * @return Returns true if both are equivalent, false otherwise
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public <T extends DataEngine<E>> boolean equivalence(T de) {
        return super.equivalence(de);
    }

    @Override
    @Behaviour(Type.MUTABLE)
    public void sort() {

    }

    /**
     * The underlying node class that controls the pointers to the {@code item}, {@code next} and {@code previous}
     * This implementation has no thread-safe mechanisms.
     * @param <E> Type argument of data
     */
    private static final class LinkedNode<E>{
        public E item;
        public LinkedNode<E> next, previous;

        public LinkedNode() {
            next = previous = null;
        }

        public void set(E item){
            this.item = Objects.requireNonNull(item);
        }

        public E item(){
            return item;
        }
    }
}
