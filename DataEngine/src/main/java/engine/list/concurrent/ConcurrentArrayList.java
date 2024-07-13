package engine.list.concurrent;

import data.constants.*;
import data.core.*;
import engine.core.AbstractList;

import java.time.chrono.MinguoDate;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Implementation(ImplementationType.IMPLEMENTATION)
@EngineNature(nature = Nature.THREAD_MUTABLE, behaviour =  EngineBehaviour.DYNAMIC, order = Ordering.UNSORTED)
public class ConcurrentArrayList<E> extends AbstractList<E> {

    private final StripedLocks stripedLock;
    private final long partition;

    //Internal buffer that fills up and flushes into the array to reduce lock contention.
    private final Object[] stripeBuffer;
    private final AtomicBoolean isBufferFlushed;
    private final AtomicInteger activeBufferCapacity;

    //Standard data-partitioning multiples, although custom values are allowed
    public static final long LOCK_STRIPING_PARTITION_512 = 512;
    public static final long LOCK_STRIPING_PARTITION_1024 = 1024;
    public static final long LOCK_STRIPING_PARTITION_2048 = 2048;

    private final AtomicInteger modCount;

    public ConcurrentArrayList() {
        this(ConcurrentArrayListTemp.LOCK_STRIPING_PARTITION_2048);
    }

    public ConcurrentArrayList(long partition){
        super(DEFAULT_CAPACITY);
        stripedLock = new StripedLocks(partition, DEFAULT_CAPACITY);
        this.partition = partition;

        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) partition];
        isBufferFlushed = new AtomicBoolean(false);
        activeBufferCapacity = new AtomicInteger(0);
    }

    /**
     * Constructor that creates a new list with the given {@code maxCapacity} and default
     * partition
     */
    public ConcurrentArrayList(int maxCapacity){
        this(maxCapacity, ConcurrentArrayListTemp.LOCK_STRIPING_PARTITION_2048);
    }

    /**
     * Constructor that creates a new list with given {@code maxCapacity} and the given
     * {@code partition}.
     */
    public ConcurrentArrayList(int maxCapacity, long partition) {
        super(maxCapacity);
        this.partition = partition;

        stripedLock = new StripedLocks(partition, maxCapacity);
        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) partition];
        isBufferFlushed = new AtomicBoolean(false);
        activeBufferCapacity = new AtomicInteger(0);
    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code list.maxCapacity() +
     * DEFAULT_CAPACITY} Uses a default value for striping partitions
     */
    public ConcurrentArrayList(AbstractList<E> list) throws ImmutableException {
        this(list, ConcurrentArrayListTemp.LOCK_STRIPING_PARTITION_2048);
    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code list.maxCapacity() +
     * DEFAULT_CAPACITY} and with the given striping {@code partition} for internal locking
     */
    public ConcurrentArrayList(AbstractList<E> list, long partition) throws ImmutableException {
        super(list.getMaxCapacity());
        addAll(list);
        this.partition = partition;

        stripedLock = new StripedLocks(partition , list.getMaxCapacity());
        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) partition];
        isBufferFlushed = new AtomicBoolean(false);
        activeBufferCapacity = new AtomicInteger(0);
        setActiveSize(list.getActiveSize());
    }

    public ConcurrentArrayList(AbstractList<E> list, int maxCapacity) throws ImmutableException {
        this(list, maxCapacity, ConcurrentArrayListTemp.LOCK_STRIPING_PARTITION_2048);
    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code maxCapacity +
     * DEFAULT_CAPACITY} and with the given {@code partition}. Throws an exception when
     * {@code maxCapacity < list.getActiveSize()}
     */
    public ConcurrentArrayList(AbstractList<E> list, int maxCapacity, long partition) throws ImmutableException {
        super(maxCapacity);
        this.partition = partition;
        stripedLock = new StripedLocks(partition, maxCapacity);
        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) partition];
        isBufferFlushed = new AtomicBoolean(false);
        activeBufferCapacity = new AtomicInteger(0);
        if(maxCapacity < list.getActiveSize()) {
            setActiveSize(0);
            throw new IndexOutOfBoundsException("Invalid capacity, Not enough space.");
        } else{
            addAll(list);
            setActiveSize(list.getActiveSize());
        }

    }

    /**
     * Constructor to add all the elements of the provided arrays Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * DEFAULT_CAPACITY} and with a default partition
     */
    public ConcurrentArrayList(E[] array) {
        this(array, ConcurrentArrayListTemp.LOCK_STRIPING_PARTITION_2048);
    }

    /**
     * Constructor to add all the elements of the provided arrays Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * DEFAULT_CAPACITY} and with the given {@code partition}
     */
    public ConcurrentArrayList(E[] array, long partition) {
        super(array.length + DEFAULT_CAPACITY);
        for(E item : array) add(item);
        stripedLock = new StripedLocks(partition, array.length);
        this.partition = partition;

        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) this.partition];
        isBufferFlushed = new AtomicBoolean(false);
        activeBufferCapacity = new AtomicInteger(0);
        setActiveSize(array.length);
    }

    /**
     * Constructor to add all the elements of the provided array. Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * maxCapacity} and with default partition. Throws an exception when {@code maxCapacity < list.getActiveSize()} Although the
     * object can be used further, it will not contain the elements in {@code array}
     */
    public ConcurrentArrayList(E[] array, int maxCapacity) {
        this(array, maxCapacity, ConcurrentArrayListTemp.LOCK_STRIPING_PARTITION_2048);
    }

    /**
     * Constructor to add all the elements of the provided array. Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * maxCapacity} and with the given {@code partition}. Throws an exception when
     * {@code maxCapacity < list.getActiveSize()} Although the object can be used further, it will
     * not contain the elements in {@code array}
     */
    public ConcurrentArrayList(E[] array, int maxCapacity, long partition) {
        super(maxCapacity);
        this.partition = partition;
        stripedLock = new StripedLocks(partition, maxCapacity);
        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) this.partition];
        isBufferFlushed = new AtomicBoolean(false);
        activeBufferCapacity = new AtomicInteger(0);
        if(maxCapacity < array.length) {
            setActiveSize(0);
            throw new IndexOutOfBoundsException("Invalid capacity, not enough space");
        } else for(E item : array) this.add(item);

    }

    @Behaviour(Type.MUTABLE)
    private void flushBuffer(){

    }

    @Behaviour(Type.IMMUTABLE)
    private void modify() {
        modCount.incrementAndGet();
    }

    @Override
    @Behaviour(Type.MUTABLE)
    public void add(E item){
        if(!isBufferFlushed.get()) flushBuffer();
        //We need the current stripe
        StripedLocks currentStripe = this.stripedLock;
        while(currentStripe.next != null) currentStripe = this.stripedLock.next;
        //Now lock and add
        try{
            currentStripe.getWriteLock().lock();
            //We always add the item to the end, i.e. the current pos of the array
            currentStripe.elements[currentStripe.activeCapacity++] = item;
            setActiveSize(getActiveSize()+1);
        }finally {
            currentStripe.getWriteLock().unlock();
            grow(); //Grow if possible
        }
    }

    @Override
    public void add(int index, E item) {
        if(!isBufferFlushed.get()) flushBuffer();

        if(index > getMaxCapacity() || index  < 0)
            throw new IndexOutOfBoundsException("Provided index is out of bounds");
        else if(getActiveSize() == getMaxCapacity())
            throw new EngineOverflowException("Overflow, engine is full");
        //Here we need to traverse the list until the stripe with the given index id present
        StripedLocks currentStripe = this.stripedLock;
        int currentSize = currentStripe.activeCapacity;
        while ((currentStripe.next != null) && (currentSize < index)){
            currentStripe = stripedLock.next;
            currentSize+=currentStripe.activeCapacity;
        }

        //We have reached the current stripe
        //Case 1
        if(currentStripe.maxCapacity < currentStripe.partition) {
            //Just add and increase size
            try {
                currentStripe.getWriteLock().lock();
                //We are sure that the index is in bounds, shift first
                currentStripe.activeCapacity++;
                for (int i = getActiveSize(); i > index; i--)
                    currentStripe.elements[(int) (i - partition)] = currentStripe.elements[(int) (i - 1 - partition)];
                currentStripe.elements[(int) (index - partition)] = item;
            } finally {
                currentStripe.getWriteLock().unlock();
                modify();
            }
            grow(); //If possible
        } else {
            //Case 2
            //This is a weird one, because we are adding into the middle of a filled stripe
            //Get the last stripe, shift items from there, it is better if it is done recursively
            StripedLocks lastStripe = this.stripedLock;
            while (lastStripe.next != null) lastStripe = lastStripe.next;
            recursiveShift(currentStripe, lastStripe, index);
        }
    }

    private void recursiveShift(StripedLocks currentStripe, StripedLocks lastStripe, int index) {
        if(!currentStripe.equals(lastStripe)){
            try {
                lastStripe.getWriteLock().lock();
                //We need to shift first
                for (int i = lastStripe.activeCapacity; i > 0; i--)
                    lastStripe.elements[i] = lastStripe.elements[i - 1];
                //After shift,we need the last item in the previous stripe
                //Obviously the previous stripe is filled
                Object item;
                try{
                    lastStripe.previous.getReadLock().lock();
                    item = lastStripe.previous.elements[(int) (partition-1)];
                }finally {
                    lastStripe.previous.getReadLock().lock();
                }
                //Shifted the item to next stripe
                lastStripe.elements[0] = item;
            }finally {
                //Now continue
                lastStripe.getWriteLock().unlock();
                recursiveShift(currentStripe, lastStripe.previous, index);
            }
        }else{
            try {
                lastStripe.getWriteLock().lock();
                //Perform a shift, no checks, we know that the last element is already shifted
                for (int i = lastStripe.activeCapacity; i > index - partition; i--)
                    lastStripe.elements[i] = lastStripe.elements[i - 1];
            }finally {
                lastStripe.getWriteLock().unlock();
            }
        }
    }

    /**
     * Adds all the items present in {@code list} into the invoking data-engine
     * This method is iterator dependent. Any implementation that is not marked with {@code Implementation}
     * must be careful to have a concrete {@code Iterator} implementation, else the method will throw an error.
     * It is highly recommended to use {@code Implementation} to guarantee that an {@code Iterator} will be
     * present during execution.
     *
     * @param list The list whose items are to be added into the invoking list
     * @throws IllegalArgumentException Thrown when the list is empty
     */
    @Override
    public <T extends AbstractList<E>> void addAll(T list) throws EngineOverflowException, ImmutableException {
        super.addAll(list);
    }

    /**
     * Similar to the {@code addAll} method, it adds all the items from the {@code start}th index, inclusive.
     * This method is iterator dependent. Any implementation that is not marked with {@code Implementation}
     * must be careful to have a concrete {@code Iterator} implementation, else the method will throw an error.
     * It is highly recommended to use {@code Implementation} to guarantee that an {@code Iterator} will be
     * present during execution.
     *
     * @param list  The list whose elements are to be added
     * @param start Starting point for adding elements, inclusive
     */
    @Override
    public <T extends AbstractList<E>> void addAllFrom(T list, int start) throws EngineOverflowException, ImmutableException {
        super.addAllFrom(list, start);
    }

    /**
     * Adds all the items lying in the range {@code start} to {@code end}. Both endpoints of interval
     * are inclusive. This method is iterator dependent. Any implementation that is not marked with {@code Implementation}
     * must be careful to have a concrete {@code Iterator} implementation, else the method will throw an error.
     * It is highly recommended to use {@code Implementation} to guarantee that an {@code Iterator} will be
     * present during execution.
     *
     * @param list  The list whose elements are to be added
     * @param start Start point for adding elements
     * @param end   End point for adding elements
     * @throws IndexOutOfBoundsException Thrown when endpoints are invalid
     */
    @Override
    public <T extends AbstractList<E>> void addAll(T list, int start, int end) throws EngineOverflowException, ImmutableException {
        super.addAll(list, start, end);
    }

    /**
     * Checks if an item is present in the invoking list
     *
     * @param item The item to be checked
     * @return Returns true if present, false otherwise
     */
    @Override
    public boolean contains(E item) {
        return false;
    }

    /**
     * Checks if all the items present in the list are present in the invoking list. This method is iterator dependent.
     * Any implementation that is not marked with {@code Implementation} must be careful to have a concrete {@code Iterator}
     * implementation, else the method will throw an error. It is highly recommended to use {@code Implementation} to
     * guarantee that an {@code Iterator} will be present during execution.
     *
     * @param list The list whose items have to be checked for presence in the invoking list
     * @return Returns true if all items are present, false otherwise
     * @throws EngineUnderflowException Thrown when sizes of both lists are different.
     */
    @Override
    public <T extends AbstractList<E>> boolean containsAll(T list) throws EngineUnderflowException {
        return super.containsAll(list);
    }

    /**
     * Checks if all the items present after the {@code start}th index are present in the invoking list. This method is
     * iterator dependent. Any implementation that is not marked with {@code Implementation} must be careful to have a
     * concrete {@code Iterator} implementation, else the method will throw an error. It is highly recommended to use
     * {@code Implementation} to guarantee that an {@code Iterator} will be present during execution.
     *
     * @param list  The list whose elements lying the provided range are to be checked
     * @param start Starting point for checking, inclusive
     * @return Returns true if all the elements present in the range are present in invoking list, false otherwise
     * @throws EngineUnderflowException  Thrown when both lists are of different sizes
     * @throws IndexOutOfBoundsException Thrown when invalid {@code start} index is passed
     */
    @Override
    public <T extends AbstractList<E>> boolean containsAllFrom(T list, int start) throws EngineUnderflowException {
        return super.containsAllFrom(list, start);
    }

    /**
     * Checks if all the items present in the given list in the range {@code start} to {@code end} inclusive is
     * present in the invoking list. This method is iterator dependent. Any implementation that is not marked with
     * {@code Implementation} must be careful to have a concrete {@code Iterator} implementation, else the method will
     * throw an error. It is highly recommended to use {@code Implementation} to guarantee that an {@code Iterator}
     * will be present during execution.
     *
     * @param list  The list whose items are to be checked in the specified range.
     * @param start Starting point for check, inclusive.
     * @param end   End point for checks, inclusive.
     * @return Returns true if all the items present in then specified range are present in the
     * invoking list, false otherwise.
     * @throws IndexOutOfBoundsException Thrown when an invalid range is passed.
     */
    @Override
    public <T extends AbstractList<E>> boolean containsAll(T list, int start, int end) {
        return super.containsAll(list, start, end);
    }

    /**
     * Removes the {@code item} if it is present in the list. All possible occurrences are removed
     *
     * @param item The item to bo removed
     * @return Returns true if removed, false otherwise.
     */
    @Override
    public boolean remove(E item) throws ImmutableException {
        return false;
    }

    /**
     * Removes the item present at the given index as long as it is not null
     *
     * @param index The position at which an item (if present) is to be removed
     * @return Returns true if an item is removed, false otherwise
     */
    @Override
    public boolean removeAt(int index) throws ImmutableException {
        return false;
    }

    /**
     * Clears all the items in the list.
     *
     * @return Returns true if cleared, false otherwise.
     */
    @Override
    public boolean clear() throws ImmutableException {
        return false;
    }

    /**
     * This method retains all the items that are present in the {@code list} passed as parameter. This method
     * is iterator dependent. Any implementation that is not marked with {@code Implementation} must be careful
     * to have a concrete {@code Iterator} implementation, else the method will throw an error. It is highly
     * recommended to use {@code Implementation} to guarantee that an {@code Iterator} will be present during
     * execution.
     *
     * @param list The list whose items are to be checked
     * @throws EngineUnderflowException Thrown when an empty list is passed
     */
    @Override
    public <T extends AbstractList<E>> void retainAll(T list) throws EngineUnderflowException, ImmutableException {
        super.retainAll(list);
    }

    /**
     * Finds the first index of the passed item in the invoking list. Will return -1 if the
     * item is not present in the list
     *
     * @param item The item whose first position is to be calculated
     * @return Returns the index position if present, else -1
     */
    @Override
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
    public E get(int index) {
        return null;
    }

    /**
     * When invoked on a data engine that implements an underlying array, will shift all the elements
     * to the beginning, i.e. a sparsely populated array can be so adjusted that all the elements
     * get move to the front
     */
    @Override
    protected void compress() throws ImmutableException {

    }

    /**
     * When the {@code activeSize} is greater than {@code GROWTh_LOAD_FACTOR * maxCapacity}, for an
     * underlying array it will end up growing by {@code Math.floor(GOLDEN_RATIO * maxCapacity)}
     * Can have an asynchronous implementation in thread-safe data engines.
     */
    @Override
    protected void grow() {

    }

    /**
     * When the {@code activeSize} is less than {@code SHRINK_LOAD_FACTOR * maxCapacity}, for an
     * underlying array it will end up shrinking by {@code Math.floor(GOLDEN_RATIO * maxCapacity)}
     * Cam have an asynchronous implementation in thread-safe data engines.
     */
    @Override
    protected void shrink() throws ImmutableException {

    }

    /**
     * @return Returns max capacity of the data engine, or the max size before growth
     */
    @Override
    public int getMaxCapacity() {
        return super.getMaxCapacity();
    }

    /**
     * Sets the new max capacity of the data-engine
     *
     * @param maxCapacity
     */
    @Override
    protected void setMaxCapacity(int maxCapacity) {
        super.setMaxCapacity(maxCapacity);
    }

    /**
     * Sets the new active size of the data-engine.
     *
     * @param activeSize
     */
    @Override
    protected void setActiveSize(int activeSize) {
        super.setActiveSize(activeSize);
    }

    /**
     * @return Returns current size of data engine
     */
    @Override
    public int getActiveSize() {
        return super.getActiveSize();
    }

    /**
     * Generates an {@code AtomicReference} of the data engine. Useful when the object has to be
     * used in a thread blocking context without making the data engine asynchronous.
     *
     * @return Returns an {@code AtomicReference} of the invoking data engine
     * @throws UnsupportedOperationException Will throw an exception when the invoking data engine is
     *                                       already {@code Thread-Mutable}
     */
    @Override
    public <T extends DataEngine<E>> AtomicReference<T> createThreadSafeImage() {
        return super.createThreadSafeImage();
    }

    /**
     * The method reverses the invoking data engine when implemented.
     *
     * @throws UnsupportedOperationException Thrown when it is unimplemented.
     */
    @Override
    public void reverse() throws ImmutableException {

    }

    @Override
    public Iterator<E> iterator() {
        return super.iterator();
    }

    /**
     * Merges the {@code de} provided with the invoking list. A new de is generated with
     * max capacity equal to sum of max-capacities of both de containing all the elements
     *
     * @param de The provided data-engine with which merging is to take place
     * @return Returns the merged data-engine
     */
    @Override
    public <T extends DataEngine<E>> T merge(T de) throws EngineUnderflowException, EngineOverflowException, ImmutableException {
        return super.merge(de);
    }

    /**
     * Merges the {@code de} provided with the invoking data-engine. Only the items present after
     * start are merged with the invoking data-engine.
     *
     * @param de    The provided data-engine with which merging is to take place
     * @param start The start point of extraction
     * @return Returns the merged data-engine
     */
    @Override
    public <T extends DataEngine<E>> T mergeFrom(T de, int start) throws EngineUnderflowException, ImmutableException {
        return super.mergeFrom(de, start);
    }

    /**
     * Merges the {@code de} provided with the invoking data-engine. Only the items present in the rang
     * {@code start} to {@code end} (inclusive) are merged with the invoking data-engine.
     *
     * @param de    The provided data-engine with which merging is to take place
     * @param start The start point of extraction
     * @param end   The end point of extraction
     * @return Returns the merged data-engine
     */
    @Override
    public <T extends DataEngine<E>> T rangeMerge(T de, int start, int end) throws EngineUnderflowException, ImmutableException {
        return super.rangeMerge(de, start, end);
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
    public E[] toArray() throws EngineUnderflowException {
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
    public E[] rangedToArray(int start) throws EngineUnderflowException {
        return super.rangedToArray(start);
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
    public E[] rangedToArray(int start, int end) throws EngineUnderflowException {
        return super.rangedToArray(start, end);
    }

    /**
     * @return Returns true if the invoking data engine has been emptied, false otherwise
     * @throws EngineUnderflowException If the data engine is empty, throws an exception
     */
    @Override
    public boolean removeAll() throws EngineUnderflowException, ImmutableException {
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
    public <T extends DataEngine<E>> boolean equals(T de) throws EngineUnderflowException {
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
    public <T extends DataEngine<E>> boolean rangeEquals(T de, int start, int end) throws EngineUnderflowException {
        return super.rangeEquals(de, start, end);
    }

    /**
     * Checks if both data engines contain the same elements, irrespective of repetitions
     *
     * @param de The data engine to be compared with
     * @return Returns true if both are equivalent, false otherwise
     */
    @Override
    public <T extends DataEngine<E>> boolean equivalence(T de) throws EngineUnderflowException {
        return super.equivalence(de);
    }

    /**
     * A doubly linked list node that contains a {@code ReentrantReadWriteLock}. This is more efficient
     * implementation compared to a single array, as a growing of array can occur in one stripe. This
     * does increase the overhead to a certain extent but is insignificant enough that it can be
     * compensated for performance.
     *
     */
    private static final class StripedLocks{

        //Lock references for this stripe
        private ReentrantReadWriteLock stripeLock;

        //Doubly-LinkedList
        public StripedLocks next;
        public StripedLocks previous;

        //A partition determines the absolute max number of elements this stripe could theoretically hold
        private final long partition;

        //Volatile due to multithreaded access
        public volatile Object[] elements;
        private volatile int maxCapacity;
        private volatile int activeCapacity;

        public StripedLocks(long partition, int maxCapacity){
            this.partition = partition;
            next = null;
            previous = null;
            stripeLock = new ReentrantReadWriteLock();
            elements = new Object[maxCapacity];
        }

        /**
         * Returns the read lock which is part of this stripe
         */
        public Lock getReadLock(){
            return this.stripeLock.readLock();
        }

        /**
         * Returns the write lock which is part of this stripe
         */
        public Lock getWriteLock(){
            return this.stripeLock.writeLock();
        }
    }
}
