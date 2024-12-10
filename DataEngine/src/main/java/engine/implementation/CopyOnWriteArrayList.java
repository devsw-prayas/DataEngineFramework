package engine.implementation;

import data.constants.*;
import data.core.*;
import data.core.ListIterator;
import data.core.RandomAccess;
import data.function.UnaryOperator;
import engine.abstraction.AbstractList;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Unlike {@link ConcurrentArrayList} this implementation creates a copy for every write operation
 * and finally locks and atomically updates the underlying array. Useful for situations when
 * reads outnumber writes significantly For fully thread-safe operations that must act directly on the
 * underlying array, {@link ConcurrentArrayList} is preferred. This implementation uses a single
 * {@link ReentrantReadWriteLock} to perform atomic locking.
 *
 * @param <E> Type of data being stored.
 *
 * @author devsw
 * @since BleedingEdge-alpha-1
 */
@Implementation(ImplementationType.IMPLEMENTATION)
@EngineNature(nature = Nature.THREAD_MUTABLE,  behaviour = EngineBehaviour.DYNAMIC, order = Ordering.UNSORTED)
public class CopyOnWriteArrayList<E> extends AbstractList<E> implements RandomAccess {

    //Copy On Write Behavior, explicit locking
    private transient Object elements[];
    private int modCount;
    private final ReentrantReadWriteLock globalLock;


    public CopyOnWriteArrayList() {
        super(DEFAULT_CAPACITY);
        elements = new Object[this.getMaxCapacity()];
        modCount = 0;
        globalLock = new ReentrantReadWriteLock();
    }

    public CopyOnWriteArrayList(int maxCapacity) {
        super(maxCapacity);
        elements = new Object[this.getMaxCapacity()];
        modCount = 0;
        globalLock = new ReentrantReadWriteLock();

    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code list.maxCapacity() +
     * DEFAULT_CAPACITY}
     */
    public CopyOnWriteArrayList(AbstractList<E> list) throws EngineOverflowException{
        super(list.getMaxCapacity());
        addAll(list);
        globalLock = new ReentrantReadWriteLock();
    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code maxCapacity +
     * DEFAULT_CAPACITY}. Throws an exception when {@code maxCapacity < list.getActiveSize()}
     */
    public CopyOnWriteArrayList(AbstractList<E> list, int maxCapacity) throws EngineOverflowException{
        super(maxCapacity);
        if(maxCapacity < list.getActiveSize())
            throw new IndexOutOfBoundsException("Invalid capacity, Not enough space.");
        else addAll(list);
        globalLock = new ReentrantReadWriteLock();
    }

    /**
     * Constructor to add all the elements of the provided arrays Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * DEFAULT_CAPACITY}
     */
    public CopyOnWriteArrayList(E[] array) throws EngineOverflowException{
        super(array.length + DEFAULT_CAPACITY);
        for(E item : array) add(item);
        globalLock = new ReentrantReadWriteLock();
    }

    /**
     * Constructor to add all the elements of the provided array. Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * maxCapacity}. Throws an exception when {@code maxCapacity < list.getActiveSize()}
     */
    public CopyOnWriteArrayList(E[] array, int maxCapacity) throws EngineOverflowException {
        super(maxCapacity);
        if(maxCapacity < array.length)
            throw new IndexOutOfBoundsException("Invalid capacity, not enough space");
        else for(E item : array) this.add(item);
        globalLock = new ReentrantReadWriteLock();
    }

    private void modify() {
        modCount++;
    }

    private Lock getReadLock(){
        return globalLock.readLock();
    }

    private Lock getWriteLock(){
        return globalLock.writeLock();
    }

    /**
     * Adds the {@code item} to the list, performs a growth if possible during additions
     *
     * @param item The item to be added
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void add(E item) {
        Object[] copy = new Object[this.getMaxCapacity()];
        System.arraycopy(elements, 0, copy, 0, elements.length);
        copy[getActiveSize()] = Objects.requireNonNull(item);
        setActiveSize(getActiveSize()+1);
        modify();
        //Obviously there's empty space, no need to worry.
        try{
            getWriteLock().lock();
            elements = copy;
        }finally {
            getWriteLock().unlock();
            grow(); //Check if a "grow" is possible
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
        //Perform a "grow" if possible
        grow();
        Object[] copy = new Object[this.getMaxCapacity()];
        System.arraycopy(elements, 0, copy, 0, elements.length);
        //Shift all the elements
        setActiveSize(getActiveSize() + 1);
        System.arraycopy(copy, index, copy, index + 1, getActiveSize() - index);
        copy[index] = Objects.requireNonNull(item);
        try {
            getWriteLock().lock();
            elements = copy;
            modify();
        }finally {
            getWriteLock().unlock();
            grow(); //May grow, who knows?
        }
    }

    /**
     * Adds all items present in {@code arr} in the range {@code start} to {@code end} inclusive. All the items
     * present must be non-null, or an exception will be thrown. Unlike other implementations this method adds
     * all the items manually to reduce contention on the single lock and the memory footprint of this method
     *
     * @param arr   An array containing non-null items to add
     * @param start Start point for adding elements
     * @param end   End point for adding elements
     * @throws NullPointerException when any of the items present in the given array is {@code null}
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void addAll(E[] arr, int start, int end) {
        if(start < 0 | start > end | end > getActiveSize())
            throw new IndexOutOfBoundsException("Index out of bounds");
        if(getActiveSize() == 0)
            throw new EngineUnderflowException("List is empty");
        Object[] copy = new Object[this.getMaxCapacity()];
        System.arraycopy(elements, 0, copy, 0, elements.length);
        for(int i = start; i < end; i++) {
            copy[i] = Objects.requireNonNull(arr[i]);
            if(load() > GROWTH_LOAD_FACTOR) grow();
        }
        try{
            getWriteLock().lock();
            elements = copy;
            modify();
        }finally {
            getWriteLock().unlock();
            grow(); //Just check
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
        try {
            getReadLock().lock();
            Objects.requireNonNull(item);
            for (int i = 0; i < getActiveSize(); i++) {
                if (elements[i].equals(item)) return true;
            }
            return false;
        }finally {
            getReadLock().unlock();
        }
    }

    /**
     * Removes the {@code item} if it is present in the list. All possible occurrences are removed
     *
     * @param item The item to bo removed
     * @return Returns true if the given {@code item} is removed
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public boolean remove(E item) {
        if (!contains(Objects.requireNonNull(item))) return false;
        else {
            Object[] copy = new Object[this.getMaxCapacity()];
            System.arraycopy(elements, 0, copy, 0, elements.length);
            for (int i = 0; i < getActiveSize(); i++) {
                if (copy[i].equals(item)) {
                    copy[i] = null;
                    modify();
                }
            }
            try{
                getWriteLock().lock();
                elements = copy;
            }finally {
                getWriteLock().unlock();
            }
            compress();
            shrink(); //Perform a shrink if possible
            setActiveSize(getActiveSize()-1);
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
        if (index > getActiveSize() - 1 | index < 0) throw new IndexOutOfBoundsException("Invalid Index");
        else if (elements[index] == null)
            return false;
        else {
            Object[] copy = new Object[getMaxCapacity()];
            System.arraycopy(elements, 0, copy, 0, elements.length);
            copy[index] = null;
            try{
                getWriteLock().lock();
                elements = copy;
            }finally {
                getWriteLock().unlock();
            }
            modify();
            compress();
            shrink(); //If possible
            setActiveSize(getActiveSize()-1);
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
        try {
            Objects.requireNonNull(item);
            getReadLock().lock();
            for (int i = 0; i < getActiveSize(); i++) if (elements[i].equals(item)) return i;
            return -1;
        }finally {
            getReadLock().lock();
        }
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
        try {
            Objects.requireNonNull(item);
            getReadLock().lock();
            for (int i = getActiveSize() - 1; i > 0; i--) if (elements[i].equals(item)) return i;
            return -1;
        }finally {
            getReadLock().lock();
        }
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
        if (index > this.getActiveSize() | index < 0)
            throw new IndexOutOfBoundsException("Invalid index");
        try {
            getReadLock().lock();
            return (E) elements[index];
        }finally {
            getReadLock().lock();
        }
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
        Object[] copy = new Object[this.getMaxCapacity()];
        System.arraycopy(elements, 0, copy, 0, elements.length);
        try {
            copy[idx] = item;
            getWriteLock().lock();
            elements = copy;
        }finally {
            getWriteLock().unlock();
        }
        modify();
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
    @SuppressWarnings("unchecked")
    public AbstractList<E> subList(int start, int end) {
        Object[] items;
        try {
            getReadLock().lock();
            if(start < 0 | end < 0 |  end - start > getActiveSize())
                throw new IndexOutOfBoundsException("Invalid range");
            if(end > getActiveSize())
                throw new IndexOutOfBoundsException("Invalid Index");
            items = new Object[end - start + 1];
            System.arraycopy(elements, start, items, 0, items.length);
        }finally {
            getReadLock().unlock();
        }
        return new CopyOnWriteArrayList<E>((E[]) items);
    }

    @Override
    @Behaviour(Type.MUTABLE)
    @SuppressWarnings("unchecked")
    public void replaceAll(UnaryOperator<E> operator, int start, int end) {
        if(end < 0 | start < 0 | start > end)
            throw new IndexOutOfBoundsException("Invalid range");
        if(end > getActiveSize())
            throw new IndexOutOfBoundsException("Invalid index");
        for (int i = start; i < end; i++)
            operator.perform((E) elements[i]);
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
        Arrays.fill(elements, currentPos, getActiveSize()-1, null);    }

    /**
     * When the {@code activeSize} is less than {@code SHRINK_LOAD_FACTOR * maxCapacity}, for an
     * underlying array it will end up shrinking by {@code Math.floor(GOLDEN_RATIO * maxCapacity)}
     * Cam have an asynchronous implementation in thread-safe data engines.
     */
    @Override
    @Behaviour(Type.MUTABLE)
    protected void shrink() {
        //First check if the array is sparsely populated
        double loadFactor = (double) getActiveSize() / getMaxCapacity();
        // Check if load factor is below the shrink factor
        if (loadFactor < SHRINK_LOAD_FACTOR) {
            modify();
            // Calculate new capacity using the golden ratio
            int newMaxCapacity = (int) (Math.floor((this.getMaxCapacity() - this.getMaxCapacity() * GOLDEN_RATIO)));
            elements = Arrays.copyOf(elements, newMaxCapacity);
            setMaxCapacity(newMaxCapacity);
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
        double loadFactor = (double) getActiveSize() / getMaxCapacity();
        // Check if load factor exceeds the threshold for growth
        if (loadFactor > GROWTH_LOAD_FACTOR) {
            modify();
            // Calculate new capacity using the golden ratio
            int newMaxCapacity = (int) Math.floor(getMaxCapacity() * GOLDEN_RATIO);
            elements = Arrays.copyOf(elements, newMaxCapacity);
            setMaxCapacity(newMaxCapacity);
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
        modify();
        Object t;
        for(int left = 0, right = this.getActiveSize() - 1;left < right; left++, right--){
            t = elements[right];
            elements[right] = elements[left];
            elements[left] = t;
        }
    }

    /**
     * In some cases, it would become convenient to use an array for iterative purposes for
     * faster run times. Thus, it becomes a better alternative to use arrays.
     *
     * @return It returns a deep-copy array view of the entire data engine
     * @throws EngineUnderflowException In case, the data engine is empty an exception is generated
     */
    @Override
    @SuppressWarnings("unchecked")
    @Behaviour(Type.IMMUTABLE)
    public E[] toArray() throws EngineUnderflowException {
        if (elements.length == 0) throw new EngineUnderflowException("List is empty");

        // Count non-null elements
        int count = 0;
        for (Object element : elements) if (element != null) count++;
        if (count == 0) throw new EngineUnderflowException("List is empty");

        // Create array of exact size
        E[] copy = (E[]) new Object[count];

        // Copy non-null elements
        int pos = 0;
        for (Object element : elements) if (element != null) copy[pos++] = (E) element;
        return copy;
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
    public E[] toArray(int start) throws EngineUnderflowException {
        return toArray(start, getActiveSize());
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
    public E[] toArray(int start, int end) throws EngineUnderflowException {
        if (this.getActiveSize() == 0) throw new EngineUnderflowException("list is empty");
        else if (end < start | end > this.getActiveSize() | start > this.getActiveSize() | end < 0 | start < 0)
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
            setActiveSize(0);
            modify();
            while (this.getMaxCapacity() > DEFAULT_CAPACITY) shrink(); //Shrink until size is near DEFAULT_CAPACITY
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
            for (int i = 0; i < this.getActiveSize(); i++) {
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
     * @throws EngineUnderflowException Thrown when either of them is empty, or range length is invalid
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    @SuppressWarnings("unchecked")
    public <T extends DataEngine<E>> boolean equals(T list, int start, int end) throws EngineUnderflowException {
        int size = this.getActiveSize();
        int size1 = list.getActiveSize();
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
            } catch (EngineOverflowException exec) {
                /*This obviously will be never hit */
                return null;
            }
        }
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
    public <T extends DataEngine<E>> T merge(T list, int start) throws EngineUnderflowException{
        if(!(list instanceof AbstractList<?>))
            throw new IllegalArgumentException("The provided data engine is not a subclass of AbstractList");
        else if(list.getActiveSize() == 0)
            throw new EngineUnderflowException("List is empty");
        else if(list.getActiveSize() < start | start < 0)
            throw new IndexOutOfBoundsException("Invalid start index");
        else {
            //Generate a copy
            E[] copy1 = list.toArray(start);
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
    public <T extends DataEngine<E>> T merge(T list, int start, int end) throws EngineUnderflowException{
        if(!(list instanceof AbstractList<?>))
            throw new IllegalArgumentException("The provided data engine is not a subclass of AbstractList");
        else if(list.getActiveSize() == 0)
            throw new EngineUnderflowException("List is empty");
        else if(list.getActiveSize() < start | list.getActiveSize() < end | start < 0 | end < 0 | end - start + 1 > list.getActiveSize())
            throw new IndexOutOfBoundsException("Invalid range index");
        else {
            //Generate a copy
            E[] copy1 = list.toArray(start, end);
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
    @Behaviour(Type.MUTABLE)
    public Iterator<E> iterator() {
        return new CopyOnWriteArrayListIterator();
    }

    /**
     * A concrete implementation of {@code Iterator} for {@code CopyOnWriteArrayList}. Uses a fail-fast
     * mechanism similar to the {@code Collections Framework}. Will throw {@code ConcurrentModificationException}
     * when alteration occurs while accessing an iterator. The iterator will self reset/
     */
    public final class CopyOnWriteArrayListIterator implements ListIterator<E> {

        private int currModCount;
        int currPos;
        CopyOnWriteArrayList<E> connectedList; //Enclosing List

        public CopyOnWriteArrayListIterator() {
            currPos = 0;
            currModCount = CopyOnWriteArrayList.this.modCount;
            this.connectedList = CopyOnWriteArrayList.this;
        }

        @Override
        public boolean hasNext() {
            return currPos < connectedList.getActiveSize();
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            if(currPos < 0)
                throw new NoSuchElementException("No more elements");
            return (E)connectedList.elements[currPos++];
        }

        @Override
        public void remove() {
            if(this.currModCount == connectedList.modCount) {
                connectedList.modify();
                currModCount++;
                //Removing item

                connectedList.elements[currPos--] = null;
                connectedList.compress(); //Perform adjustment
                connectedList.modify();
                currModCount++;

                connectedList.shrink();
                connectedList.modify();
                currModCount++;
            }else {
                //Performing an auto reset
                this.currModCount = connectedList.modCount;
                this.currPos = 0;
                throw new ConcurrentModificationException("Alteration occurred iterator access");
            }
        }

        /**
         * Checks if an item is present before the current item
         */
        @Override
        public boolean hasPrevious() {
            return currPos > 0;
        }

        /**
         * Returns the previous item present before the current item
         */
        @Override
        @SuppressWarnings("unchecked")
        public E previous() {
            if(!hasPrevious())
                throw new NoSuchElementException("No more elements");
            if(modCount != currModCount)
                throw new ConcurrentModificationException("Alteration occurred iterator access");
            return (E)connectedList.elements[currPos--];
        }

        /**
         * Sets the current item to the given {@code item}
         */
        @Override
        public void set(E item) {
            if(modCount != currModCount)
                throw new ConcurrentModificationException("Alteration occurred during iterator access");
            connectedList.elements[currPos] = item;
            modify();
            currModCount++;
        }
    }
}