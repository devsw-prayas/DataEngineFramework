package engine.list.concurrent;

import data.constants.*;
import data.core.*;
import engine.core.AbstractList;
import engine.list.DynamicArrayList;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

/**
 * A fully dynamic array-list implementation that is completely thread-safe. An iterator for this data-engine
 * will generate an entire new copy of the underlying array and atomically update the original reference to the
 * array. Useful when read attempts out-number write attempts. Similar to {@code DynamicArrayList}. Implementation
 * uses an array of {@code Object}s, to allow a simpler implementation. Null items are not allowed in the list.
 * In case a null object is passed, methods which add to the list will throw {@code NullPointerException}
 * This concurrent data-structure utilizes lock-striping through the internal {@code StripedLock} which makes it
 * possible for read operations to fail.
 *
 * @param <E> Type of data being stored.
 *
 * @author devsw
 * @since BleedingEdge-alpha-1
 */
@Implementation(ImplementationType.IMPLEMENTATION)
@EngineNature(nature = Nature.THREAD_MUTABLE,  behaviour = EngineBehaviour.DYNAMIC, order = Ordering.UNSORTED)
public class CopyOnWriteArrayList<E> extends AbstractList<E> {

    //Fully multithreaded implementation
    private volatile transient Object[] elements;
    private final StripedLock stripedLock;
    private final long partition;

    //Atomic Behavior required with theses
    private final AtomicInteger activeSize;
    private final AtomicInteger maxCapacity;

    //Standard data-partitioning multiples, although custom values are allowed
    public static final long LOCK_STRIPING_PARTITION_512 = 512;
    public static final long LOCK_STRIPING_PARTITION_1024 = 1024;
    public static final long LOCK_STRIPING_PARTITION_2048 = 2048;

    /**
     * Internal lock striping class that allows for more fine-grain control over multithreading
     * Utilizes {@code StampedLock}s for lock striping and due to its optimistic-read behavior.
     * Declared private as it has little to no value beyond the context of this class.
     */
    private final class StripedLock {
        private final DynamicArrayList<StampedLock> locks;

        public StripedLock() {
            locks = new DynamicArrayList<>();
            locks.add(new StampedLock());
        }

        /**
         * Simply adds a new {@code StampedLock}
         */
        public void addNewLock() {
            locks.add(new StampedLock());
        }

        /**
         * Redundant locks get removed. The total stripes along with one additional stripe is retained.
         */
        public void removeRedundantStripes(){
            int length = CopyOnWriteArrayList.this.getActiveSize();
            long striped = length / CopyOnWriteArrayList.this.partition, stripeDiff = locks.getActiveSize() - striped;
            if(stripeDiff != 0 & length % CopyOnWriteArrayList.this.partition > 0)
                for (long i = striped; i < locks.getActiveSize(); i++) locks.removeAt((int)i); //Retain stipe
            else if (stripeDiff != 0)
                for (long i = striped-1; i < locks.getActiveSize(); i++) locks.removeAt((int)i);
        }

        /**
         * Returns the lock present at the given stripe-index
         */
        public StampedLock getStripeLock(final int index) {
            return locks.get((int) (index / CopyOnWriteArrayList.this.partition));
        }
    }

    public CopyOnWriteArrayList() {
        this(CopyOnWriteArrayList.LOCK_STRIPING_PARTITION_1024);
    }

    public CopyOnWriteArrayList(long partition){
        super(DEFAULT_CAPACITY);
        elements = new Object[this.getMaxCapacity()];
        stripedLock = new StripedLock();
        this.partition = partition;

        maxCapacity = new AtomicInteger(DEFAULT_CAPACITY);
        activeSize = new AtomicInteger(0);
    }

    public CopyOnWriteArrayList(int maxCapacity){
        this(maxCapacity, CopyOnWriteArrayList.LOCK_STRIPING_PARTITION_1024);
    }

    public CopyOnWriteArrayList(int maxCapacity, long partition) {
        super(maxCapacity);
        elements = new Object[this.getMaxCapacity()];
        this.partition = partition;

        stripedLock = new StripedLock();
        this.maxCapacity = new AtomicInteger(maxCapacity);
        activeSize = new AtomicInteger(0);
    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code list.maxCapacity() +
     * DEFAULT_CAPACITY} Uses a default value for striping partitions
     */
    public CopyOnWriteArrayList(AbstractList<E> list) throws EngineOverflowException, ImmutableException {
        this(list, CopyOnWriteArrayList.LOCK_STRIPING_PARTITION_1024);
    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code list.maxCapacity() +
     * DEFAULT_CAPACITY} and with the given striping {@code partition} for internal locking
     */
    public CopyOnWriteArrayList(AbstractList<E> list, long partition) throws EngineOverflowException, ImmutableException {
        super(list.getMaxCapacity());
        addAll(list);
        this.partition = partition;

        stripedLock = new StripedLock();
        maxCapacity = new AtomicInteger(getMaxCapacity());
        activeSize = new AtomicInteger(0);
    }

    public CopyOnWriteArrayList(AbstractList<E> list, int maxCapacity) throws EngineOverflowException, ImmutableException {
        this(list, maxCapacity, CopyOnWriteArrayList.LOCK_STRIPING_PARTITION_1024);
    }

        /**
         * Constructor to add all the elements of the provided list. Empty lists are not allowed.
         * Creates it with all the elements and max capacity equal to the provided {@code maxCapacity +
         * DEFAULT_CAPACITY}. Throws an exception when {@code maxCapacity < list.getActiveSize()}
         */
    public CopyOnWriteArrayList(AbstractList<E> list, int maxCapacity, long partition) throws EngineOverflowException, ImmutableException {
        super(maxCapacity);
        this.partition = partition;
        if(maxCapacity < list.getActiveSize())
            throw new IndexOutOfBoundsException("Invalid capacity, Not enough space.");
        else addAll(list);

        stripedLock = new StripedLock();
        this.maxCapacity = new AtomicInteger(maxCapacity);
        activeSize = new AtomicInteger(0);
    }

    /**
     * Constructor to add all the elements of the provided arrays Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * DEFAULT_CAPACITY}
     */
    public CopyOnWriteArrayList(E[] array) throws EngineOverflowException, ImmutableException {
        super(array.length + DEFAULT_CAPACITY);
        for(E item : array) add(item);
        stripedLock = new StripedLock();
        partition = CopyOnWriteArrayList.LOCK_STRIPING_PARTITION_1024; //Temporary

        maxCapacity = new AtomicInteger(array.length + DEFAULT_CAPACITY);
        activeSize = new AtomicInteger(0);
    }

    /**
     * Constructor to add all the elements of the provided array. Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * maxCapacity}. Throws an exception when {@code maxCapacity < list.getActiveSize()} Although the
     * object can be used further, it will not contain the elements in {@code array}
     */
    public CopyOnWriteArrayList(E[] array, int maxCapacity) {
        super(maxCapacity);
        partition = CopyOnWriteArrayList.LOCK_STRIPING_PARTITION_1024; //Temporary
        if(maxCapacity < array.length)
            throw new IndexOutOfBoundsException("Invalid capacity, not enough space");
        else for(E item : array) this.add(item);

        stripedLock = new StripedLock();
        this.maxCapacity = new AtomicInteger(maxCapacity);
        activeSize = new AtomicInteger(0);
    }

    /**
     * Adds the {@code item} to the list, performs a growth if possible during additions
     *
     * @param item The item to be added
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void add(E item) {
        //Perform a growth if possible
        grow();
        //Obviously there's empty space, no need to worry.
        if (item == null) throw new NullPointerException("Item is null");
        else {
            this.elements[activeSize.incrementAndGet()] = item;
            grow(); //Check once again
        }
    }

    /**
     * Adds the provided {@code item} to the list at the (if valid) {@code index}, will perform a "grow"
     * if possible
     *
     * @param index Position at which the item is to be added
     * @param item  The item to be added
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void add(int index, E item) {
        //Here index must be valid
        if (index < 0 | index > this.getActiveSize())
            throw new IndexOutOfBoundsException("Index out of bounds");
        else if (item == null) throw new NullPointerException("Item is null");
        else {
            //Perform a "grow" if possible
            grow();

            //Shift all the elements
            activeSize.incrementAndGet();
            for (int i = activeSize.get(); i > index; i--)
                elements[i] = elements[i - 1];
            elements[index] = item;
            grow(); //May grow, who knows?
        }
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
        for (Object element : elements) {
            if (element.equals(item)) {
                return true;
            }
        }
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
        if (!contains(item)) return false;
        else {
            for (int i = 0; i < activeSize.get(); i++) {
                if (elements[i].equals(item)) {
                    elements[i] = null;
                }
            }
            compress();
            shrink(); //Perform a shrink if possible
            activeSize.decrementAndGet();
            return true;
        }
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
        if (index > activeSize.get() - 1 | index < 0) throw new IndexOutOfBoundsException("Invalid Index");
        else if (elements[index] == null)
            return false;
        else {
            elements[index] = null;
            compress();
            shrink(); //If possible
            activeSize.decrementAndGet();
            return true;
        }
    }

    /**
     * Clears all the items in the list.
     *
     * @return Returns true if cleared, false otherwise.
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public boolean clear() {
        return removeAll();
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
        for (int i = 0; i < activeSize.get(); i++) if (elements[i].equals(item)) return i;
        return -1;
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
        for (int i = activeSize.get() - 1; i > 0; i--) if (elements[i].equals(item)) return i;
        return -1;
    }

    /**
     * Returns the item present at the given {@code index}, if valid.
     *
     * @param index The position of retrieval of item
     * @return Returns the item present at the given index.
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    @SuppressWarnings("unchecked")
    public E get(int index) {
        if (index > this.activeSize.get() | index < this.activeSize.get())
            throw new IndexOutOfBoundsException("Invalid index");
        return (E) elements[index];
    }

    /**
     * When invoked on a data engine that implements an underlying array, will shift all the elements
     * to the beginning, i.e. a sparsely populated array can be so adjusted that all the elements
     * get move to the front
     */
    @Override
    @Behaviour(Type.MUTABLE)
    protected void compress() {
        int currentPos = 0;
        for (int i = 0; i < elements.length; i++)
            if (elements[i] != null) elements[currentPos++] = elements[i];

        for (int i = currentPos; i < elements.length; i++) elements[i] = null;
    }

    /**
     * When the {@code activeSize} is less than {@code SHRINK_LOAD_FACTOR * maxCapacity}, for an
     * underlying array it will end up shrinking by {@code Math.floor(GOLDEN_RATIO * maxCapacity)}
     * Cam have an asynchronous implementation in thread-safe data engines.
     */
    @Override
    @Behaviour(Type.MUTABLE)
    protected void shrink() {
        //First check if the array is sparsely populated
        if ((activeSize.get() * 1.0) / maxCapacity.get() < SHRINK_LOAD_FACTOR) {
            //Then shrink it
            int newMaxCapacity = (int) (Math.floor((this.maxCapacity.get() - this.maxCapacity.get() * GOLDEN_RATIO)));
            try {
                E[] temp = this.toArray();
                this.elements = Arrays.copyOf(temp, newMaxCapacity);
            } catch (EngineUnderflowException exec) { /*Unreachable code */ }
        }
    }

    /**
     * When the {@code activeSize} is greater than {@code GROWTh_LOAD_FACTOR * maxCapacity}, for an
     * underlying array it will end up growing by {@code Math.floor(GOLDEN_RATIO * maxCapacity)}
     * Cam have an asynchronous implementation in thread-safe data engines.
     */
    @Override
    @Behaviour(Type.MUTABLE)
    protected void grow() {
        //First check if load is high enough to warrant a growth
        if ((activeSize.get() * 1.0) / maxCapacity.get() > GROWTH_LOAD_FACTOR) {
            //Then perform growth
            int newMaxCapacity = (int) Math.floor((this.maxCapacity.get() * GOLDEN_RATIO));
            try {
                E[] copy = this.toArray();
                elements = Arrays.copyOf(copy, newMaxCapacity);
            } catch (EngineUnderflowException exec) {/*Unreachable code*/}
        }
    }

    /**
     * The method reverses the invoking data engine when implemented.
     *
     * @throws UnsupportedOperationException Thrown when it is unimplemented.
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void reverse() {
        int left = 0, right = this.activeSize.get() - 1;
        Object t;
        while (left < right) {
            t = elements[right];
            elements[right] = elements[left];
            elements[left] = t;
            left++;
            right--;
        }
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
    @SuppressWarnings("unchecked")
    @Behaviour(Type.IMMUTABLE)
    public E[] toArray() throws EngineUnderflowException {
        if (elements.length == 0) throw new EngineUnderflowException("List is empty");
        E[] copy = (E[]) new Object[elements.length];
        int pos = 0;
        for (Object element : elements)
            if (element != null) copy[pos++] = (E) element;
        return Arrays.copyOf(copy, pos);
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
    @SuppressWarnings("unchecked")
    public E[] rangedToArray(int start) throws EngineUnderflowException {
        if (this.getActiveSize() == 0) throw new EngineUnderflowException("List is empty");
        else if (this.getActiveSize() < start) throw new IndexOutOfBoundsException("Invalid start position");
        E[] copy = (E[]) new Object[this.activeSize.get() - start];
        for (int i = start - 1; i < this.activeSize.get(); i++)
            copy[i - start + 1] = this.get(i);
        return Arrays.copyOf(copy, activeSize.get() - start);
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
    @SuppressWarnings("unchecked")
    public E[] rangedToArray(int start, int end) throws EngineUnderflowException {
        if (this.getActiveSize() == 0) throw new EngineUnderflowException("list is empty");
        else if (end < start | end > this.activeSize.get() | start > this.activeSize.get() | end < 0 | start < 0)
            throw new IndexOutOfBoundsException("Invalid subarray range");
        E[] copy = (E[]) new Object[end - start];
        for (int i = start - 1; i < end; i++)
            copy[i - start + 1] = this.get(i);
        return Arrays.copyOf(copy, end - start);
    }

    /**
     * @return Returns true if the invoking data engine has been emptied, false otherwise
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public boolean removeAll() {
        if (elements == null || getActiveSize() == 0)
            return false;
        else {
            elements = new Object[getMaxCapacity()];
            this.activeSize.set(0);
            while (this.maxCapacity.get() > DEFAULT_CAPACITY) shrink(); //Shrink until size is near DEFAULT_CAPACITY
            return true;
        }
    }

    /**
     * Checks if the invoking data engine and the data engine passed are truly equal, i.e. positions of all
     * elements are identical
     *
     * @param list The data engine to be compared
     * @return Returns true if both are equals, false otherwise
     * @throws EngineUnderflowException Thrown when either of them is empty, or if both are of different lengths
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public <T extends DataEngine<E>> boolean equals(T list) throws EngineUnderflowException {
        if (!(list instanceof AbstractList<?>)) throw new IllegalArgumentException("The provided list " +
                "must be a subclass of AbstractList");
        else if (this.getActiveSize() != list.getActiveSize())
            return false;
        else if (this.getActiveSize() == 0 | list.getActiveSize() == 0)
            throw new EngineUnderflowException("Either of the lists is empty");
        else {
            for (int i = 0; i < this.activeSize.get(); i++) {
                if (this.get(i) != ((AbstractList<?>) list).get(i))
                    return false;
            }
        }
        return true;
    }

    /**
     * Checks within an exclusive-bounded range the equality of the given data engine and the invoking
     * data engine. Behavior similar to {@code equals}
     *
     * @param list  The data engine to be compared with me
     * @param start The starting position, exclusive of range
     * @param end   The ending position, exclusive of range
     * @return Returns true if the range are equal for both
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    @SuppressWarnings("unchecked")
    public <T extends DataEngine<E>> boolean rangeEquals(T list, int start, int end) {
        int size = this.getActiveSize();
        int size1 = this.getActiveSize();
        if (!(list instanceof AbstractList<?>))
            throw new IllegalArgumentException("The list passed must be a subclass of AbstractList");
        else if (start > size1 | end > size1 | (end - start + 1) > size1)
            throw new IndexOutOfBoundsException("Invalid range: Range beyond passed list");
        else if (start > size | end > size | (end - start + 1) > size)
            throw new IndexOutOfBoundsException("Invalid range: Range beyond invoking list");
        else if (start < 0 | end < 0)
            throw new IndexOutOfBoundsException("Invalid index passed");
        else {
            int block = 0;
            for (E item : (AbstractList<E>) list) {
                if (block >= start && block <= end) {
                    if (!this.get(block).equals(item)) {
                        return false;
                    }
                }
                block++;
            }
        }
        return true;
    }

    /**
     * Checks if both data engines contain the same elements, irrespective of repetitions
     *
     * @param list The data engine to be compared with
     * @return Returns true if both are equivalent, false otherwise
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    @SuppressWarnings("unchecked")
    public <T extends DataEngine<E>> boolean equivalence(T list) throws EngineUnderflowException {
        if (!(list instanceof AbstractList<?>))
            throw new IllegalArgumentException("The passed list must be a subclass of AbstractList");
        else return list.getActiveSize() == this.getActiveSize() & containsAll((AbstractList<E>) list);
    }

    /**
     * Merges the {@code list} provided with the invoking list. A new list is generated with
     * max capacity equal to sum of max-capacities of both list containing all the elements
     *
     * @param list The provided data-engine with which merging is to take place
     * @return Returns the merged data-engine
     */
    @Override
    @SuppressWarnings("unchecked")
    @Behaviour(Type.MUTABLE)
    public <T extends DataEngine<E>> T merge(T list) throws EngineUnderflowException {
        if (!(list instanceof AbstractList<?>))
            throw new IllegalArgumentException("The provided data engine is not a subclass of AbstractList");
        else if (list.getActiveSize() == 0)
            throw new EngineUnderflowException("List is empty");
        else {
            //Generate a copy
            E[] copy1 = list.toArray();
            //Generate another copy
            E[] copy2 = this.toArray();
            //Now generate a new one
            //This method might be inefficient!
            try {
                CopyOnWriteArrayList<E> temp = new CopyOnWriteArrayList<>(copy1);
                //Now inject the rest
                for (E item : copy2) temp.add(item);
                return (T) (new CopyOnWriteArrayList<>(temp, this.getMaxCapacity() + list.getMaxCapacity()));
            } catch (EngineOverflowException exec) { /*This obviously will be never hit */ } catch (ImmutableException e) {
                throw new RuntimeException(e);
            }
        }
        return null; //Unreachable
    }

    /**
     * Merges the {@code de} provided with the invoking data-engine. Only the items present after
     * start are merged with the invoking data-engine.
     *
     * @param list The provided data-engine with which merging is to take place
     * @param start The start point of extraction
     * @return Returns the merged data-engine
     */
    @Override
    @SuppressWarnings("unchecked")
    @Behaviour(Type.MUTABLE)
    public <T extends DataEngine<E>> T mergeFrom(T list, int start) throws EngineUnderflowException, ImmutableException {
        if(!(list instanceof AbstractList<?>))
            throw new IllegalArgumentException("The provided data engine is not a subclass of AbstractList");
        else if(list.getActiveSize() == 0)
            throw new EngineUnderflowException("List is empty");
        else if(list.getActiveSize() < start | start < 0)
            throw new IndexOutOfBoundsException("Invalid start index");
        else {
            //Generate a copy
            E[] copy1 = list.rangedToArray(start);
            //Generate another copy
            E[] copy2 = this.toArray();

            //Now generate a new one
            //This method might be inefficient!
            try {
                CopyOnWriteArrayList<E> temp = new CopyOnWriteArrayList<>(copy1);
                //Now inject the rest
                for (E item : copy2) temp.add(item);
                return (T) (new CopyOnWriteArrayList<>(temp,
                        this.getMaxCapacity() + list.getMaxCapacity() - start));
            } catch (EngineOverflowException exec) { /*This obviously will be never hit */ }
        }
        return null; //Unreachable
    }

    /**
     * Merges the {@code list} provided with the invoking data-engine. Only the items present in the rang
     * {@code start} to {@code end} (inclusive) are merged with the invoking data-engine.
     *
     * @param list The provided data-engine with which merging is to take place
     * @param start The start point of extraction
     * @param end   The end point of extraction
     * @return Returns the merged data-engine
     */
    @Override
    @SuppressWarnings("unchecked")
    @Behaviour(Type.MUTABLE)
    public <T extends DataEngine<E>> T rangeMerge(T list, int start, int end) throws EngineUnderflowException, ImmutableException {
        if(!(list instanceof AbstractList<?>))
            throw new IllegalArgumentException("The provided data engine is not a subclass of AbstractList");
        else if(list.getActiveSize() == 0)
            throw new EngineUnderflowException("List is empty");
        else if(list.getActiveSize() < start | list.getActiveSize() < end | start < 0 | end < 0 | end - start + 1 > list.getActiveSize())
            throw new IndexOutOfBoundsException("Invalid range index");
        else {
            //Generate a copy
            E[] copy1 = list.rangedToArray(start, end);
            //Generate another copy
            E[] copy2 = this.toArray();
            //Now generate a new one
            //This method might be inefficient!
            try {
                CopyOnWriteArrayList<E> temp = new CopyOnWriteArrayList<>(copy1);
                //Now inject the rest
                for (E item : copy2) temp.add(item);
                return (T) (new CopyOnWriteArrayList<>(temp,
                        this.getMaxCapacity() + list.getMaxCapacity()));
            } catch (EngineOverflowException exec) { /*This obviously will be never hit */ }
        }
        return null; //Unreachable
    }

    @Override
    public Iterator<E> iterator() {
        try {
            return new CopyOnWriteArrayListIterator();
        } catch (EngineUnderflowException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A concrete implementation of {@code Iterator} for {@code CopyOnWriteArrayList}. Uses a copy-on-write
     * mechanism for uninterrupted iterative behaviour. A new copy is generated every time a new iterator is
     * generated, it will contain a snapshot of the underlying array at that moment.
     */
    public final class CopyOnWriteArrayListIterator implements Iterator<E> {

        //Internal Array
        private final E[] arraySnapshot;
        int currPos;

        public CopyOnWriteArrayListIterator() throws EngineUnderflowException {
            currPos = 0;
            arraySnapshot = CopyOnWriteArrayList.this.toArray();
        }

        @Override
        public boolean hasNext() {
            return currPos < arraySnapshot.length;
        }

        @Override
        public E next() {
            return arraySnapshot[currPos++];
        }
    }
}
