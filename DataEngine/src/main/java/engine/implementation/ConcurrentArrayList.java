package engine.implementation;

import data.constants.*;
import data.core.*;
import data.core.ListIterator;
import data.function.UnaryOperator;
import engine.abstraction.AbstractList;


import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A high performance implementation of a fully thread-safe dynamic array-list. {@code ConcurrentArrayList}
 * uses {@link ReentrantReadWriteLock}s upon segmented arrays each having a length {@code partition} long.
 * The individual stripes are managed by an internal private class {@link LockedStripes} which takes care of
 * operations that span across a single-stripe. The list itself carefully manages these operations and
 * makes them span across multiple stripes without various side effects. Though slower than a regular
 * non-thread safe implementation, it is preferred due to its safe operations in highly contented situations
 * involving multiple threads. Certain methods act on the entire the internal doubly linked list and any
 * other methods when called during batching will simply return. This implementation is one of the few
 * engines that use {@link Sortable}. It is due to its inherent nature that performing a sort externally
 * will cause more problems. Thus, a custom sorting algorithm is warranted. This implementation uses
 * {@link ConcurrentDualPivotQuickSort}, a high performance optimized version of DualPivotQuicksort
 * <p>
 * It is necessary that {@link #isBatching()} is called to check before performing any modifications
 *
 * @param <E> Type argument of stored data
 * @author Devsw
 */
@Implementation(ImplementationType.IMPLEMENTATION)
@EngineNature(nature = Nature.THREAD_MUTABLE, behaviour =  EngineBehaviour.DYNAMIC, order = Ordering.UNSORTED)
public class ConcurrentArrayList<E> extends AbstractList<E> implements Sortable {

    private LockedStripes lockedStripe;
    private final long partition;

    //Internal buffer that fills up and flushes into the array to reduce lock contention.
    private final Object[] stripeBuffer;
    private final AtomicBoolean isBufferFlushed;
    private volatile int activeBufferCapacity;

    //Standard data-partitioning multiples, although custom values are allowed
    public static final long LOCK_STRIPING_PARTITION_512 = 512;
    public static final long LOCK_STRIPING_PARTITION_1024 = 1024;
    public static final long LOCK_STRIPING_PARTITION_2048 = 2048;

    private double reductionFactor = 1.0;

    private boolean isBatching = false; //Important when batched operations are occurring

    private final AtomicInteger modCount;

    public ConcurrentArrayList() {
        this(ConcurrentArrayList.LOCK_STRIPING_PARTITION_2048);
    }

    public ConcurrentArrayList(long partition){
        super(DEFAULT_CAPACITY);
        lockedStripe = new LockedStripes(partition, DEFAULT_CAPACITY);
        this.partition = partition;

        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) partition];
        isBufferFlushed = new AtomicBoolean(true);
        activeBufferCapacity = 0;
    }

    /**
     * Constructor that creates a new list with the given {@code maxCapacity} and default
     * partition
     */
    public ConcurrentArrayList(int maxCapacity){
        this(maxCapacity, ConcurrentArrayList.LOCK_STRIPING_PARTITION_2048);
    }

    /**
     * Constructor that creates a new list with given {@code maxCapacity} and the given
     * {@code partition}.
     */
    public ConcurrentArrayList(int maxCapacity, long partition) {
        super(maxCapacity);
        this.partition = partition;

        lockedStripe = new LockedStripes(partition, maxCapacity);
        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) partition];
        isBufferFlushed = new AtomicBoolean(true);
        activeBufferCapacity = 0;
    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code list.maxCapacity() +
     * DEFAULT_CAPACITY} Uses a default value for striping partitions
     */
    public ConcurrentArrayList(AbstractList<E> list) {
        this(list, ConcurrentArrayList.LOCK_STRIPING_PARTITION_2048);
    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code list.maxCapacity() +
     * DEFAULT_CAPACITY} and with the given striping {@code partition} for internal locking
     */
    public ConcurrentArrayList(AbstractList<E> list, long partition) {
        super(list.getMaxCapacity());
        addAll(list);
        this.partition = partition;

        lockedStripe = new LockedStripes(partition , list.getMaxCapacity());
        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) partition];
        isBufferFlushed = new AtomicBoolean(true);
        activeBufferCapacity = 0;
        setActiveSize(list.getActiveSize());
    }

    public ConcurrentArrayList(AbstractList<E> list, int maxCapacity) {
        this(list, maxCapacity, ConcurrentArrayList.LOCK_STRIPING_PARTITION_2048);
    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code maxCapacity +
     * DEFAULT_CAPACITY} and with the given {@code partition}. Throws an exception when
     * {@code maxCapacity < list.getActiveSize()}
     */
    public ConcurrentArrayList(AbstractList<E> list, int maxCapacity, long partition) {
        super(maxCapacity);
        this.partition = partition;
        lockedStripe = new LockedStripes(partition, maxCapacity);
        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) partition];
        isBufferFlushed = new AtomicBoolean(true);
        activeBufferCapacity = 0;
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
        this(array, ConcurrentArrayList.LOCK_STRIPING_PARTITION_2048);
    }

    /**
     * Constructor to add all the elements of the provided arrays Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * DEFAULT_CAPACITY} and with the given {@code partition}
     */
    public ConcurrentArrayList(E[] array, long partition) {
        super(array.length + DEFAULT_CAPACITY);
        for(E item : array) add(item);
        lockedStripe = new LockedStripes(partition, array.length);
        this.partition = partition;

        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) this.partition];
        isBufferFlushed = new AtomicBoolean(true);
        activeBufferCapacity = 0;
        setActiveSize(array.length);
    }

    /**
     * Constructor to add all the elements of the provided array. Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * maxCapacity} and with default partition. Throws an exception when {@code maxCapacity < list.getActiveSize()} 
     * Although the object can be used further, it will not contain the elements in {@code array}
     */
    public ConcurrentArrayList(E[] array, int maxCapacity) {
        this(array, maxCapacity, ConcurrentArrayList.LOCK_STRIPING_PARTITION_2048);
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
        lockedStripe = new LockedStripes(partition, maxCapacity);
        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) this.partition];
        isBufferFlushed = new AtomicBoolean(true);
        activeBufferCapacity = 0;
        if(maxCapacity < array.length) {
            setActiveSize(0);
            throw new IndexOutOfBoundsException("Invalid capacity, not enough space");
        } else for(E item : array) this.add(item);
    }

    /**
     * This method must be called to check if batched operations or global operations are occurring on
     * a separate thread. It is to make sure data is consistent during batched operations
     */
    public boolean isBatching(){
        return isBatching;
    }

    /**
     * Internal helper method, flushes the buffer into the list
     */
    private void flushBuffer(){
        //Start flushing, get current stripe
        LockedStripes lastStripe = lockedStripe;
        while (lastStripe.next != null) lastStripe = lastStripe.next;
        //Now start adding
        recursiveFill(lastStripe, activeBufferCapacity);
        modify();
    }

    /**
     * Internal helper method, recursively flushes the buffer
     */
    private void recursiveFill(LockedStripes stripe, int nums){
        int temp = nums;
        try{
            stripe.getWriteLock().lock();
            while (stripe.maxCapacity < partition){
                stripe.elements[stripe.activeCapacity] = stripeBuffer[nums - temp--];
                synchronized (this){
                    stripe.activeCapacity++;
                }
                if(load() > GROWTH_LOAD_FACTOR) grow();
            }
        }finally {
            stripe.getWriteLock().unlock();
        }
        if(nums > 0)
            recursiveFill(stripe, nums);
    }


    @Behaviour(Type.IMMUTABLE)
    private void modify() {
        modCount.incrementAndGet();
    }

    /**
     * When invoked on a data engine that implements an underlying array, will shift all the elements
     * to the beginning, i.e. a sparsely populated array can be so adjusted that all the elements
     * get move to the front
     */
    @Override
    @Behaviour(Type.MUTABLE)
    protected void compress() {
        //Runs the recursive form
        shift(lockedStripe, lockedStripe.next);
        modify();
    }

    /**
     * Internal helper method, recursively adjusts all the stripes for empty indices.
     */
    private void shift(LockedStripes currentStripe, LockedStripes nextStripe){
        if(!(nextStripe == null)){
            //We are sure that we are somewhere in the interior off the array
            try{
                currentStripe.getWriteLock().lock();
                nextStripe.getWriteLock().lock();
                //Now check the number of nulls
                int nulls = (int) Arrays.stream(currentStripe.elements).filter(Objects::nonNull).count();
                for(int i = 0; i < currentStripe.elements.length; i++)
                    //We know the partition is complete
                    if(currentStripe.elements[i] == null) nulls++;

                //Performing compression
                currentStripe.compressStripe();
                nextStripe.compressStripe();


                if(nulls > 0){
                    //So get the items from the next stripe
                    int pos = (int) (partition - 1 - nulls);
                    System.arraycopy(nextStripe.elements, 0, currentStripe.elements, pos, nulls);
                    //Now null the elements and shift
                    Arrays.fill(nextStripe.elements, 0, nulls-1,null);
                    nextStripe.compressStripe();
                }
            }finally {
                currentStripe.getWriteLock().unlock();
                nextStripe.getWriteLock().unlock();
            }
            shift(nextStripe, nextStripe.next);
        } else{
            //We are at the final stripe
            currentStripe.compressStripe();
        }
    }

    /**
     * When the {@code activeSize} is greater than {@code GROWTh_LOAD_FACTOR * maxCapacity}, for an
     * underlying array it will end up growing by {@code Math.floor(GOLDEN_RATIO * maxCapacity)}
     */
    @Override
    @Behaviour(Type.MUTABLE)
    protected void grow() {
        if(load() < GROWTH_LOAD_FACTOR) return;
        //We need to make sure where the last stripe lies
        LockedStripes lastStripe = this.lockedStripe;
        while (lastStripe.next != null) lastStripe = lastStripe.next;
        int newMaxCapacity = (int) Math.floor(getMaxCapacity() * GOLDEN_RATIO * reductionFactor);
        double strength = 0.998;
        reductionFactor *= strength;
        int diff = newMaxCapacity - getMaxCapacity();

        //Case 1
        if(lastStripe.maxCapacity < partition){

            //Check if current stripe has enough space
            if(lastStripe.maxCapacity + diff < partition){
                //Perform a "grow"
                try {
                    lastStripe.getWriteLock().lock();
                    synchronized (this) {
                        lastStripe.elements = Arrays.copyOf(lastStripe.elements, lastStripe.maxCapacity + diff);
                        lastStripe.maxCapacity = lastStripe.maxCapacity + diff;
                    }
                }finally {
                    lastStripe.getWriteLock().unlock();
                }
            }else{
                int rem = (int) (lastStripe.maxCapacity + diff - partition);
                recursiveGrow(lastStripe, rem);
            }
        }else{
            //Here the stripe is fully filled
            recursiveGrow(lastStripe, diff);
        }
        setMaxCapacity(newMaxCapacity);
        modify();
    }

    /**
     * Internal helper method, assumes the stripes previous have been maxed out and further growth is possible
     * when moving to new stripes
     */
    private void recursiveGrow(LockedStripes lastStripe, int rem){
        if(rem > partition){
            int diff = (int)(rem - partition);
            try{
                lastStripe.getWriteLock().lock();
                synchronized (this) {
                    lastStripe.elements = Arrays.copyOf(lastStripe.elements, (int) partition);
                }
                lastStripe.maxCapacity = (int) partition;
                lastStripe.next = new LockedStripes(partition, (int) partition);
                lastStripe.next.previous = lastStripe;
            }finally {
                lastStripe.getWriteLock().unlock();
                recursiveGrow(lastStripe.next, diff);
            }
        }else {
            try{
                lastStripe.getWriteLock().lock();
                synchronized (this) {
                    lastStripe.elements = Arrays.copyOf(lastStripe.elements, (int) partition);
                }
                lastStripe.maxCapacity = (int) partition;
                lastStripe.next = new LockedStripes(partition, rem);
                lastStripe.next.previous = lastStripe;
            }finally {
                lastStripe.getWriteLock().unlock();
            }
        }
    }

    /**
     * When the {@code activeSize} is less than {@code SHRINK_LOAD_FACTOR * maxCapacity}, for an
     * underlying array it will end up shrinking by {@code maxCapacity}
     */
    @Override
    @Behaviour(Type.MUTABLE)
    protected void shrink() {
        if(load() > SHRINK_LOAD_FACTOR) return;

        //Since we are shrinking, and we know that all the null indices are at last
        //The best way to approach would be again from last, removing nulls until the size is
        //equal to newMaxCapacity
        LockedStripes lastStripe = this.lockedStripe, temp = null;
        while (lastStripe.next != null) lastStripe = lastStripe.next;
        int newMaxCapacity = (int) (Math.floor((getMaxCapacity() * GOLDEN_RATIO - getMaxCapacity())));

        //Start removing from end
        int removed = getMaxCapacity() - newMaxCapacity;
        do{
            if(lastStripe.activeCapacity == 0){ //Empty stripe removed
                //Now remove
                try {
                    temp = lastStripe;
                    lastStripe.getWriteLock().lock();
                    removed -= lastStripe.maxCapacity;
                    lastStripe = lastStripe.previous;
                }finally {
                    temp.getWriteLock().unlock();
                    temp = null;
                }
            } else if(lastStripe.maxCapacity > removed){
                //We need to just remove shrink the array
                //We are sure the active size is sufficiently low
                try {
                    lastStripe.getWriteLock().lock();
                    synchronized (this) {
                        lastStripe.elements = Arrays.copyOf(lastStripe.elements, lastStripe.maxCapacity - removed);
                    }
                    removed = 0;
                }finally {
                    lastStripe.getWriteLock().unlock();
                }
            }
        }while (removed != 0);
        modify();
    }

    /**
     * Adds an element to the last stripe of the list.
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void add(E item) {
        if (!isBufferFlushed.get()) flushBuffer();
        //We need the current stripe
        LockedStripes currentStripe = this.lockedStripe;
        while(currentStripe.next != null){
            if(currentStripe.activeCapacity < currentStripe.maxCapacity) break;
            currentStripe = currentStripe.next;
        }

        //Now lock and add
        try {
            currentStripe.getWriteLock().lock();
            //We always add the item to the end, i.e. the current pos of the array
            currentStripe.elements[currentStripe.activeCapacity] = item;
            synchronized (this) {
                currentStripe.activeCapacity++;
            }
            setActiveSize(getActiveSize() + 1);
        } finally {
            currentStripe.getWriteLock().unlock();
            grow(); //Grow if possible
        }
        modify();
    }

    /**
     * Adds an item at the given index. If it is part of the last stripe. It shifts the elements
     * In case the index lies in the middle of a filled stripe, elements are shifted through the stripes
     * recursively, until an empty index is obtained.
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void add(int index, E item) {
        if(!isBufferFlushed.get()) flushBuffer();
        if(index > getMaxCapacity() || index  < 0)
            throw new IndexOutOfBoundsException("Provided index is out of bounds");
        else if(getActiveSize() == getMaxCapacity())
            throw new EngineOverflowException("Overflow, engine is full");
        //Here we need to traverse the list until the stripe with the given index id present
        LockedStripes currentStripe = this.lockedStripe;
        int currentSize = currentStripe.activeCapacity;
        while ((currentStripe.next != null) && (currentSize < index)){
            currentStripe = lockedStripe.next;
            currentSize+=currentStripe.activeCapacity;
        }

        //We have reached the current stripe
        //Case 1
        if(currentStripe.maxCapacity < currentStripe.partition) {
            //Just add and increase size
            try {
                currentStripe.getWriteLock().lock();
                //We are sure that the index is in bounds, shift first
                synchronized (this){
                    currentStripe.activeCapacity++;
                }
                System.arraycopy(currentStripe.elements, index, currentStripe.elements, index+1, getActiveSize() - index);
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
            LockedStripes lastStripe = this.lockedStripe;
            while (lastStripe.next != null) lastStripe = lastStripe.next;
            recursiveShift(currentStripe, lastStripe, index);
            //Now add the element
            try{
                currentStripe.getWriteLock().lock();
                currentStripe.elements[index - currentSize] = item;
            }finally {
                currentStripe.getWriteLock().unlock();
            }
            synchronized (this) {
                lastStripe.activeCapacity++;
            }
            grow();
        }
        modify();
    }

    /**
     * Internal helper method, shifts all the elements through the stripes in a recursive manner.
     */
    private void recursiveShift(LockedStripes currentStripe, LockedStripes lastStripe, int index) {
        if(!currentStripe.equals(lastStripe)){
            try {
                lastStripe.getWriteLock().lock();
                //We need to shift first
                System.arraycopy(lastStripe.elements, 1, lastStripe.elements, 0, lastStripe.activeCapacity-1);
                //After shift, we need the last item in the previous stripe
                //Obviously the previous stripe is filled
                Object item;
                try{
                    lastStripe.previous.getReadLock().lock();
                    item = lastStripe.previous.elements[(int) (partition-1)];
                }finally {
                    lastStripe.previous.getReadLock().unlock();
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
                System.arraycopy(lastStripe.elements, 1, lastStripe.elements, 0, lastStripe.activeCapacity - index);
            }finally {
                lastStripe.getWriteLock().unlock();
            }
        }
    }

    /**
     * Adds all the items present in {@code list} into the invoking data-engine
     * This specific implementation is not iterator dependent due the striped nature of the underlying array.
     * As the entire array is segmented, it becomes necessary to carefully add items. Additional stripes are assumed
     * to be added inside {@code grow}
     *
     * @param list The list whose items are to be added into the invoking list
     * @throws IllegalArgumentException Thrown when the list is empty
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public <T extends AbstractList<E>> void addAll(T list){
        addAll(list, 0 , list.getActiveSize());
    }

    /**
     * Similar to the {@code addAll} method, it adds all the items from the {@code start}th index, inclusive.
     * This specific implementation is not iterator dependent due the striped nature of the underlying array.
     * As the entire array is segmented, it becomes necessary to carefully add items. Additional stripes are assumed
     * to be added inside {@code grow}
     *
     * @param list  The list whose elements are to be added
     * @param start Starting point for adding elements, inclusive
     * @throws IllegalArgumentException Thrown when the list is empty
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public <T extends AbstractList<E>> void addAll(T list, int start) {
        addAll(list, start, list.getActiveSize());
    }

    /**
     * Adds all the items lying in the range {@code start} to {@code end}. Both endpoints of interval
     * are inclusive. This specific implementation is not iterator dependent due the striped nature of the underlying
     * array. As the entire array is segmented, it becomes necessary to carefully add items. Additional stripes are
     * assumed to be added inside {@code grow}
     *
     * @param list  The list whose elements are to be added
     * @param start Start point for adding elements
     * @param end   End point for adding elements
     * @throws IndexOutOfBoundsException Thrown when endpoints are invalid
     * @throws IllegalArgumentException Thrown when the list is empty
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public <T extends AbstractList<E>> void addAll(T list, int start, int end){
        if((end - start + 1) > list.getActiveSize())
            throw new IndexOutOfBoundsException("Passed range exceeds number of items present");
        else if(end > list.getActiveSize() | start > list.getActiveSize() |
                start > end | start < 0 | end < 0)
            throw new IndexOutOfBoundsException("Range is invalid");
        else if(list.getActiveSize() == 0)
            throw new IllegalArgumentException("Provided list is empty");

        while(!isBufferFlushed.get()) flushBuffer();

        //Here we have to keep on adding elements until the stripe is exhausted
        LockedStripes lastStripe = this.lockedStripe;
        while (lastStripe.next != null) lastStripe = lastStripe.next;

        int pos = start;
        while(pos < end) {
            //We obviously will begin with last stripe
            try {
                lastStripe.getWriteLock().lock();
                for (int i = lastStripe.activeCapacity; i < lastStripe.maxCapacity; i++) {
                    if (pos < end) {
                        lastStripe.elements[i] = list.get(pos++);
                        synchronized (this) {
                            lastStripe.activeCapacity++;
                        }
                        setActiveSize(getActiveSize() + 1);
                    }
                    if (load() > GROWTH_LOAD_FACTOR) grow(); //Making sure growth occurs
                }
            } finally {
                lastStripe.getWriteLock().unlock();
                modify();
            }
            //Now if the stripe is filled to up
            if(lastStripe.activeCapacity == partition && pos < end)
                //Get next stripe, continue until it is finished
                lastStripe = lastStripe.next;
        }
    }

    /**
     * Adds all items present in {@code arr} in the range {@code start} to {@code end} inclusive. All the items
     * present must be non-null, or an exception will be thrown. This thread-safe implementation utilizes the internal
     * lock-stripe elements. It doesn't depend on {@code add} due to lock contention. This uses the same code as
     * used by {@code addAll}.
     *
     * @param arr   An array containing non-null items to add
     * @param start Start point for adding elements
     * @param end   End point for adding elements
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void addAll(E[] arr, int start, int end) {
        if(start > end | start < 0 | end < 0 | end > arr.length | start > arr.length)
            throw new IndexOutOfBoundsException("Provided range is invalid");
        else if(Arrays.stream(arr).anyMatch(Objects::isNull))
            throw new NullPointerException("Null elements are not allowed");
        else if(arr.length == 0)
            throw new IllegalArgumentException("Array is empty");
        else{
            while(!isBufferFlushed.get()) flushBuffer();
            int pos = start;
            //Here we have to keep on adding elements until the stripe is exhausted
            LockedStripes lastStripe = this.lockedStripe;
            while (lastStripe.next != null) lastStripe = lastStripe.next;
            while(pos < end) {
                //We obviously will begin with last stripe
                try {
                    int emptyIdx = lastStripe.maxCapacity - lastStripe.activeCapacity;
                    int copies = Math.min(emptyIdx, end - pos);
                    lastStripe.getWriteLock().lock();
                    System.arraycopy(arr, pos, lastStripe.elements, lastStripe.activeCapacity, copies);

                    if (load() > GROWTH_LOAD_FACTOR) grow(); //Making sure growth occurs
                    pos+=emptyIdx;
                    synchronized (this){
                        lastStripe.activeCapacity+=emptyIdx;
                    }
                } finally {
                    lastStripe.getWriteLock().unlock();
                    modify();
                }
                //Now if the stripe is filled to up
                if(lastStripe.activeCapacity == partition && pos < end)
                    //Get next stripe, continue until it is finished
                    lastStripe = lastStripe.next;
            }
        }
    }

    /**
     * Adds an item into the internal {@code stipeBuffer}. The internal buffer is flushed into the main stripes
     * only when other methods that directly act on the stripes are called. It is not possible to directly
     * call {@code flushBuffer}. This method is more efficient when consecutive calls are required, and it is too
     * expensive to repeatedly use the internal stripes. It will auto flush when the buffer is full
     */
    @Behaviour(Type.MUTABLE)
    public void offer(E item){
        if(activeBufferCapacity == partition){
            flushBuffer();
            activeBufferCapacity = 0;
        }
        //Add to the buffer
        stripeBuffer[activeBufferCapacity] = item;
        isBufferFlushed.set(false);
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
        LockedStripes currentStripe = lockedStripe;
        while (currentStripe.next != null){
            if(currentStripe.contains(item)) return true;
            currentStripe = currentStripe.next;
        }
        return false;
    }


    /**
     * Checks if all the items present in the given list in the range {@code start} to {@code end} inclusive is
     * present in the invoking list. This method is iterator dependent. Any implementation that is not marked with
     * {@link Implementation} must be careful to have a concrete {@link Iterator} implementation, else the method will
     * throw an error. It is highly recommended to use {@link Implementation} to guarantee that an {@link Iterator}
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
    @Behaviour(Type.IMMUTABLE)
    public <T extends AbstractList<E>> boolean containsAll(T list, int start, int end) {
        if(start > end | start < 0 | end < 0 | end > list.getActiveSize()| start > list.getActiveSize())
            throw new IndexOutOfBoundsException("Provided range is invalid");
        //We can simply run through each stripe.
        LockedStripes currentStripe = lockedStripe;
        int blocking = 0;
        for(E item : list) {
            if(blocking++ < start) continue;
            if(blocking > end) break;
            if(!currentStripe.contains(item)) return false;
            if(blocking % partition == 0) currentStripe = currentStripe.next;
        }
        return true;
    }


    /**
     * Removes the {@code item} if it is present in the list. All possible occurrences are removed
     *
     * @param item The item to bo removed
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public boolean remove(E item) {
        int changes = 0, pos, stripe = 0, prevStripe = stripe;
        LockedStripes currStripe = lockedStripe;
        try{
            currStripe.getWriteLock().lock();
            for(pos = 0; pos < getActiveSize(); pos++){
                if(currStripe.elements[pos].equals(item)){
                    currStripe.elements[pos] = null;
                    changes++;
                }
                stripe = (int) (pos / partition);
                if(stripe > prevStripe){
                    prevStripe = stripe;
                    //Release current stripe
                    currStripe.getWriteLock().unlock();
                    currStripe = currStripe.next;
                    currStripe.getWriteLock().lock();
                }
            }
        }finally {
            currStripe.getWriteLock().unlock();
            modify();
        }
        if(changes > 0) {
            compress();
            return true;
        }else return false;
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
        if(index > getActiveSize() || index < 0) return false;
        else {
            int pos = 0, stripe = 0;
            LockedStripes currentStripe = lockedStripe;
            while(index > pos){
                pos+= currentStripe.activeCapacity;
                currentStripe = currentStripe.next;
                stripe++;
            }
            //Now at the required point
            try{
                currentStripe.getWriteLock().lock();
                currentStripe.elements[(int) (index - stripe * partition)] = null;
            }finally {
                currentStripe.getWriteLock().unlock();
                compress();
                modify();
            }
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
     * This method retains all the items that are present in the {@code list} passed as parameter. This method
     * does not depend on iterators. Instead, it acts directly on the stripes, letting other methods like
     * {@link #remove} and {@link #contains} do the magic
     *
     * @param list The list whose items are to be checked
     */
    @Override
    @Behaviour(Type.MUTABLE)
    @SuppressWarnings("unchecked")
    public <T extends AbstractList<E>> void retainAll(T list) {
        LockedStripes currentStripe = lockedStripe;
        while(currentStripe.next != null){
            try{
                currentStripe.getReadLock().lock();
                //Check
                for(Object item : currentStripe.elements)
                    if(!list.contains((E) item)) remove((E) item);
            }finally {
                currentStripe.getReadLock().unlock();
                currentStripe = currentStripe.next;
            }
        }
        modify();
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
        if(item == null)
            throw new NullPointerException("Item passed is null");
        else if(getActiveSize() == 0)
            throw new EngineUnderflowException("List is empty");
        int index = -1;
        LockedStripes currentStripe = lockedStripe;
        outer : while(currentStripe.next != null){
            try {
                currentStripe.getReadLock().lock();
                for (int i = 0; i < currentStripe.activeCapacity; i++) {
                    if (currentStripe.elements[i].equals(item)) break outer;
                    index++;
                }
            }finally {
                currentStripe.getReadLock().unlock();
            }
        }
        return index;
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
        int index = -1;
        if(item == null) throw new NullPointerException("Item passed is null");
        else if(!contains(item)) return index;
        LockedStripes currentStripe = lockedStripe;
        while(currentStripe.next != null) currentStripe = currentStripe.next;
        //Now we simply initialize
        index = getActiveSize();
        outer : while(currentStripe.previous != null){
            try{
                currentStripe.getReadLock().lock();
                for(int i = currentStripe.activeCapacity-1; i >= 0; i--) {
                    index--;
                    if (currentStripe.elements[i].equals(item)) break outer;
                }
            }finally {
                currentStripe.getReadLock().unlock();
            }
        }
        return index;
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
        if(index > getActiveSize() || index < 0)
            throw new IndexOutOfBoundsException("Index out of bounds");
        int pos = 0;
        LockedStripes currentStripe = lockedStripe;
        while(index > pos){
            currentStripe = currentStripe.next;
            pos+= currentStripe.activeCapacity;
        }
        //We are at the required stripe actually
        try{
            currentStripe.getReadLock().lock();
            int val = pos - index, relativePos = currentStripe.activeCapacity - val;
            return (E) currentStripe.elements[val];
        }finally{
            currentStripe.getReadLock().unlock();
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
        if(idx > getActiveSize() || idx < 0)
            throw new IndexOutOfBoundsException("Index out of bounds");
        //Now get the stripe containing idx
        LockedStripes currentStripe = lockedStripe;
        int pos = 0;
        while(idx > pos){
            pos+= currentStripe.activeCapacity;
            currentStripe = currentStripe.next;
        }
        //Now subtract and adjust
        idx -= (--pos) * (int)partition;
        try {
            currentStripe.getWriteLock().lock();
            currentStripe.elements[idx] = item;
        }finally {
            currentStripe.getWriteLock().unlock();
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
    public AbstractList<E> subList(int start, int end)   {
        if(start > end | start < 0 | end < 0)
            throw new IndexOutOfBoundsException("Invalid range parameters");
        AbstractList<E> list = new ConcurrentArrayList<>();
        int blocked = 0;
        for (E e : this) {
            if(blocked >= start && blocked <= end) {
                list.add(e);
                blocked++;
            }
        }
        return list;
    }

    /**
     * Performs the operation defined by {@code operator} on all the items lying in the range {@code start}
     * to {@code end}. The actual elements in the underlying lock-striped list are modified.
     * This method may not complete in case any concurrent modifications occur.
     * @param operator An {@link UnaryOperator} that is applied on all the elements present in the range
     * @param start Starting index
     * @param end End index
     * @throws ImmutableException Thrown when the implementation is immutable.
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void replaceAll(UnaryOperator<E> operator, int start, int end)   {
        if (isBatching) return;
        isBatching = true;
        if(start > end | start < 0 | end < 0)
            throw new IndexOutOfBoundsException("Invalid range parameters");
        else if(end > getActiveSize())
            throw new IndexOutOfBoundsException("Invalid range parameters");
        int block = 0;
        ListIterator<E> concurrent = this.concurrentIterator();
        while(concurrent.hasNext()){
            if(block > start & block < end)
                operator.perform(concurrent.next());
            block++;
        }
        isBatching = false;
    }


    /**
     * Generates an {@link AtomicReference} of the data engine. Useful when the object has to be
     * used in a thread blocking context without making the data engine asynchronous.
     *
     * @return Returns an {@link AtomicReference} of the invoking data engine
     * @throws UnsupportedOperationException Will throw an exception when the invoking data engine is
     * already {@code Thread-Mutable}
     */
    @Override
    @Behaviour(Type.UNSUPPORTED)
    public <T extends DataEngine<E>> AtomicReference<T> createThreadSafeImage() {
        throw new UnsupportedOperationException("Implementation is fully thread safe");
    }

    /**
     * This implementation of reverse is a bit different from non-thread safe implementations. This is due to
     * the striped nature of the underlying array. Each individual stripe reversed and the linked list is simply
     * rearranged
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void reverse() {
        if(isBatching()) return; //Don't want the thread to be blocked
        isBatching = true;
        if(getActiveSize() == 0){
            return;
        }else if(lockedStripe.next == null) //A single stripe
            lockedStripe.reverse();
        else{
            //This is a good one. We know that there's an extra stripe that has some elements
            LockedStripes lastStripe = lockedStripe;
            while (lastStripe.next != null) lastStripe = lastStripe.next;
            LockedStripes temp = new LockedStripes(partition, lastStripe.maxCapacity);
            //Now copy all the first elements from first stripe to the temp
            try{
                temp.getWriteLock().lock();
                lockedStripe.getReadLock().lock();
                System.arraycopy(lockedStripe.elements, 0, temp.elements, 0, lockedStripe.activeCapacity);
                //Now we add the stripe to the front
                lockedStripe.previous = temp;
                temp.next = lockedStripe;
            }finally {
                lockedStripe.getReadLock().unlock();
                temp.getWriteLock().unlock();
            }
            int shift = lastStripe.activeCapacity;
            //Here we have to simply shift elements from the beginning towards the new stripe so that the last
            //stripe becomes obsolete. We operate on two stripes at once
            temp = lockedStripe;
            while(temp.next != null){
                try{
                    temp.getWriteLock().lock();
                    temp.next.getReadLock().lock();
                    //First shift current stripe
                    System.arraycopy(temp.elements, shift, temp.elements, 0, (int) (partition -shift));
                    //Now shift from the next
                    System.arraycopy(temp.next.elements, 0, temp.elements,
                            (int) (partition -shift), shift);
                    temp = temp.next;
                }finally {
                    temp.getWriteLock().unlock();
                    temp.next.getReadLock().unlock();
                }
            }

            //Now rearrange stripes
            lastStripe = lockedStripe;
            while(lastStripe.next.next != null) lastStripe = lastStripe.next; //Get to the last
            lastStripe.next = null;
            lockedStripe = lockedStripe.previous; //Adjust the stripes

            //Now reverse it
            lastStripe = lockedStripe;
            temp = null;
            while(lastStripe != null){
                temp = lastStripe.previous;
                lastStripe.previous = lastStripe.next;
                lastStripe.next = temp;

                lastStripe = lastStripe.previous;
            }

            if(temp != null){
                lockedStripe = temp.previous;
            }
        }
        isBatching = false;
        modify();
    }

    /**
     * Merges the {@code list} provided with the invoking list. A new list is generated with
     * max capacity equal to sum of max-capacities of both list containing all the elements
     *
     * @param list The provided data-engine with which merging is to take place
     * @return Returns the merged data-engine
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public <T extends DataEngine<E>> T merge(T list)   {
        return merge(list, 0 , list.getActiveSize());
    }

    /**
     * Merges the {@code list} provided with the invoking data-engine. Only the items present after
     * start are merged with the invoking data-engine.
     *
     * @param list    The provided data-engine with which merging is to take place
     * @param start The start point of extraction
     * @return Returns the merged data-engine
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public <T extends DataEngine<E>> T merge(T list, int start)   {
        return merge(list, start, list.getActiveSize());
    }

    /**
     * Merges the {@code list} provided with the invoking data-engine. Only the items present in the rang
     * {@code start} to {@code end} (inclusive) are merged with the invoking data-engine.
     *
     * @param list    The provided data-engine with which merging is to take place
     * @param start The start point of extraction
     * @param end   The end point of extraction
     * @return Returns the merged data-engine
     */
    @Override
    @SuppressWarnings("unchecked")
    @Behaviour(Type.IMMUTABLE)
    public <T extends DataEngine<E>> T merge(T list, int start, int end)   {
        //This is a bit of a weird one because we have a lock-stripe segmented array and some type of list.
        if(!isBufferFlushed.get()) flushBuffer();
        if(!(list instanceof AbstractList<?>))
            throw new IllegalArgumentException("The provided data engine is not a subclass of AbstractList");
        else if(list.getActiveSize() == 0)
            throw new EngineUnderflowException("List is empty");
        else if(list.getActiveSize() < start | list.getActiveSize() < end | start < 0 | end < 0 | end - start + 1 > list.getActiveSize())
            throw new IndexOutOfBoundsException("Invalid range index");
        else{
            //Here the best way would be to use arrays
            E[] self = this.toArray();
            E[] provided = list.toArray(start, end);

            ConcurrentArrayList<E> merged = new ConcurrentArrayList<>(self);
            merged.addAll(provided);
            return (T) merged;
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
    @Behaviour(Type.IMMUTABLE)
    public E[] toArray(){
        return toArray(0, getActiveSize());
    }


    /**
     * Similar to the {@code toArray} method, it creates an array from all object from {@code start} to {@code end}
     * inclusive. Can throw {@link IndexOutOfBoundsException} when invalid index is passed
     *
     * @param start The starting point for extraction
     * @param end   The end point of extraction
     * @return Returns an array containing the required elements
     * @throws EngineUnderflowException Thrown when invoking data engine is empty
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    @SuppressWarnings("unchecked")
    public E[] toArray(final int start, final int end){
        if(start > end | start > getActiveSize() | end > getActiveSize())
            throw new IllegalArgumentException("Invalid range");
        else if (start < 0 | end < 0)
            throw new IndexOutOfBoundsException("Negative index passed");
        int items = end - start;
        Object[] array = new Object[items];
        int arrayPos = 0;

        LockedStripes currentStripe = lockedStripe;
        int currentIdx = 0;
        while(currentStripe != null && currentIdx < end){
            try{
                currentStripe.getReadLock().lock();
                int stripeEndIdx = currentIdx + currentStripe.activeCapacity;
                if(end <= stripeEndIdx){
                    int relativeStart = start - currentIdx;
                    int relativeEnd = end - currentIdx;
                    int length = relativeEnd - relativeStart;
                    System.arraycopy(currentStripe.elements, relativeStart, array, arrayPos, length);
                    break;
                }else if(start < stripeEndIdx){
                    int relativeStart = start - currentIdx;
                    int length = currentStripe.activeCapacity - relativeStart;
                    System.arraycopy(currentStripe.elements, relativeStart, array, arrayPos, length);
                    arrayPos+=length;
                }
            }finally {
                currentStripe.getReadLock().unlock();
            }
            currentIdx += currentStripe.activeCapacity;
            currentStripe = currentStripe.next;
        }
        return (E[]) array;
    }

    /**
     * @return Returns true if the invoking list has been emptied, false otherwise
     * @throws EngineUnderflowException If the  is empty, throws an exception
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public boolean removeAll(){
        if(!isBufferFlushed.get()) flushBuffer();
        if(getActiveSize() == 0) return true;
        //Now start from last stripe, and clear each stripe
        LockedStripes finalStripe = lockedStripe;
        while (finalStripe.next != null) finalStripe = finalStripe.next;
        //Now go in reverse
        LockedStripes temp;
        while(finalStripe.previous != null){
            finalStripe.next = null;
            temp = finalStripe.previous;
            finalStripe.previous = null;
            finalStripe = temp;
        }
        modify();
        return true;
    }

    /**
     * Checks if the invoking data engine and the data engine passed are truly equal, i.e. positions of all elements
     * are identical
     *
     * @param list The data engine to be compared
     * @return Returns true if both are equals, false otherwise
     * @throws EngineUnderflowException Thrown when either of them is empty, or if both are of different lengths
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public <T extends DataEngine<E>> boolean equals(T list){
        return equals(list, 0 , list.getActiveSize());
    }

    /**
     * Checks within an exclusive-bounded range the equality of the given data engine and the invoking
     * data engine. Behaviour similar to {@code equals}
     *
     * @param list    The data engine to be compared with me
     * @param start The starting position, exclusive of range
     * @param end   The ending position, exclusive of range
     * @return Returns true if the range are equal for both
     * @throws EngineUnderflowException Thrown when either of them is empty, or range length is invalid
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public <T extends DataEngine<E>> boolean equals(T list, int start, int end){
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
        else{
            for (int block = start; block < end; block++){
                if(!this.get(block).equals(((AbstractList<?>) list).get(block)))
                    return false;
            }
            return true;
        }
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
    public <T extends DataEngine<E>> boolean equivalence(T list){
        if(!(list instanceof AbstractList))
            throw new IllegalArgumentException("The list passed must be a subclass of AbstractList");
        if(list.isEmpty())
            throw new IllegalArgumentException("The list passed cannot be empty");
        return this.containsAll((AbstractList<E>)list);
    }

    @Override
    @Behaviour(Type.IMMUTABLE)
    public Iterator<E> iterator() {
        return new SnapshotIterator();
    }

    @Behaviour(Type.MUTABLE)
    public ListIterator<E> concurrentIterator(){
        return new ConcurrentIterator();
    }

    @Override
    @Behaviour(Type.IMMUTABLE)
    public void sort(Comparator<?> comparator) {
        //TODO
    }

    /**
     * Implements a highly efficient implementation of Dual Pivot Quicksort. This class is built to easily
     * sort a single stripe. At the end all stripes are merged through multiway merging. This is an example
     * of implementation of a custom sorting algorithm for a specialized structure.
     */
    @SuppressWarnings("unchecked")
    private final class ConcurrentDualPivotQuickSort{
        private int MAX_INVERSIONS;
        Comparator<? super E> comparator;
        public ConcurrentDualPivotQuickSort(Comparator<? super E> comparator){
            this.comparator = comparator;
        }

        /**
         * The base method that runs it on a given stripe.
         * It obtains a write lock and starts the sorting
         * @param stripe The stripe to be sorted
         */
        public void sort(LockedStripes stripe){
            try{
                stripe.getWriteLock().lock();
                MAX_INVERSIONS = (int)(stripe.activeCapacity * 0.05);
                if(stripe.activeCapacity <= 1) return;
                else if(stripe.activeCapacity == 2) {
                    //Compare and swap
                    if (comparator.compare((E) stripe.elements[0], (E) stripe.elements[1]) > 0)
                        swap(stripe.elements, 0, 1);
                } else if (isReverseSorted(stripe.elements, 0 , stripe.activeCapacity-1)) {
                    //Simply reverse it
                    reverse(stripe.elements, 0 , stripe.activeCapacity-1);
                } else if (isSorted(stripe.elements, 0 , stripe.activeCapacity-1)) {
                    return;
                } else if (isNearlySorted(stripe.elements, 0 , stripe.activeCapacity-1)) {
                    //Faster for nearly sorted arrays
                    insertionSort(stripe.elements, 0 , stripe.activeCapacity-1);
                } else if(stripe.activeCapacity <= 16){
                    //Direct Insertion sort
                    insertionSort(stripe.elements, 0, stripe.activeCapacity-1);
                } else quicksort(stripe.elements, 0, stripe.elements.length - 1);
            } finally {
                stripe.getWriteLock().unlock();
            }
        }

        /**
         * Method where the magic happens. A simple Dual Pivot Quicksort implementation
         * @param array The array for sorting
         * @param low Lower pivot
         * @param high Upper pivot
         */
        private void quicksort(Object[] array, int low, int high){
            if(high - low < 17){
                //Insertion sort for 16 elements
                insertionSort(array, low, high);
                return;
            } else if(!(low < high)) return;

            //Swap if the ordering is wrong
            if(comparator.compare((E)array[low], (E)array[high]) > 0){
                swap(array, low, high);
            }

            //Get pivot elements
            Object rightPivot = array[high];
            Object leftPivot = array[low];

            int i = low + 1;
            int j = high - 1;
            int k = low + 1;

            //Swapping based on ranges
            while(k < j){
                if(comparator.compare((E)array[k] , (E)leftPivot) <= 0){
                    swap(array, k++, i++);
                }else if(comparator.compare((E)array[k] , (E)rightPivot) >= 0){
                    swap(array, k++, j--);
                }else k++;
            }

            //Three partitions
            quicksort(array, low, i - 1);
            quicksort(array, i + 1, j - 1);
            quicksort(array, j + 1, high);
        }

        //Fallback insertion sort for 16 or less elements
        private void insertionSort(Object[] array, int start, int end){
            if(array.length <= 1) return;
            for(int i = start+1; i < end; i++){
                Object element = array[i];
                int j = i - 1;

                while(j >= start && comparator.compare((E)element, (E)array[j]) > 0){
                    array[j+1] = array[j];
                    j--;
                }
                array[j+1] = element;
            }
        }

        //Simple swap method
        private void swap(Object[] array, int i, int j){
            Object temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }

        private int medianOfThree(Object[] array, int low, int mid, int high){
            if(comparator.compare((E)array[low], (E)array[mid]) > 0)swap(array, low, mid);
            if (comparator.compare((E)array[mid], (E)array[high]) > 0)swap(array, high, mid);
            if (comparator.compare((E)array[low], (E)array[mid]) > 0)swap(array, low, mid);
            return mid;
        }

        //Checks if it is already sorted
        private boolean isSorted(Object[] array, int low, int high){
            for(int i = low + 1; i <= high; i++){
                if(comparator.compare((E)array[i-1], (E)array[i]) > 0)
                    return false;
            }
            return true;
        }

        //Checks for a reverse
        private boolean isReverseSorted(Object[] array, int low, int high){
            for(int i = low + 1; i <= high; i++){
                if(comparator.compare((E)array[i-1], (E)array[i]) > 0)
                    return false;
            }
            return true;
        }

        //Simple reverse
        private void reverse(Object[] array, int low, int high){
            while(low < high){
                swap(array, low++, high--);
            }
        }

        //Checks if number of inversions present is greater than that of MAX_INVERSIONS
        private boolean isNearlySorted(Object[] array, int low, int high){
            int inversions = 0;
            for(int i = low + 1; i <= high; i++){
                if(comparator.compare((E)array[i-1], (E)array[i]) > 0){
                    inversions++;
                }
            }

            return inversions < MAX_INVERSIONS;
        }
    }

    /**
     * A snapshot iterator which is bidirectional. The default {@code iterator} method will return a snapshot
     * iterator. Any modifications occurring after the snapshot will not be reflected in the iterator. For
     * such an iterator it is recommended to use the {@link ConcurrentIterator}
     */
    public final class SnapshotIterator implements Iterator<E>{

        private final E[] snapshot;
        int currPos = 0;

        public SnapshotIterator(){
            snapshot = ConcurrentArrayList.this.toArray();
        }

        @Override
        public boolean hasNext() {
            return currPos < snapshot.length;
        }

        @Override
        public E next() {
            return snapshot[currPos++];
        }

        public boolean hasPrevious(){
            return currPos > -1;
        }

        public E previous(){
            return snapshot[currPos--];
        }
    }

    /**
     * A more advanced {@link Iterator} that is hooked directly into the actual list. Due to its concurrent nature
     * it is useful when direct access to the list is necessary. Methods can throw {@link java.util.ConcurrentModificationException}
     * when any  modifications occur during iteration. The iterator wil auto reset in such cases. The list is
     * bidirectional in nature.
     *
     * @author devsw
     */
    public class ConcurrentIterator implements ListIterator<E> {
        private LockedStripes activeStripe;
        private int stripeIdx, actualIdx;
        private int currCount;

        public ConcurrentIterator(){
            activeStripe = lockedStripe;
            actualIdx = 0;
            stripeIdx = 0;
            currCount = modCount.get();
        }

        @Override
        public boolean hasNext() {
            if(currCount != modCount.get()) throw new ConcurrentModificationException("Internal array" +
                    "modified");
            return actualIdx < getActiveSize();
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            if(currCount != modCount.get()) throw new ConcurrentModificationException("Internal array " +
                    "modified");
            if(!hasNext()) throw new NoSuchElementException("No more items present");
            E item;
            int relative = actualIdx - (int) (stripeIdx * partition);
            try{
                activeStripe.getReadLock().lock();
                item = (E) activeStripe.elements[relative];
            }finally {
                activeStripe.getReadLock().unlock();
            }
            actualIdx++;
            //Now if possible, shift to next stripe
            if(relative+1 > activeStripe.activeCapacity){
                activeStripe = activeStripe.next;
                stripeIdx++;
            }
            return item;
        }

        @Override
        public void remove() {
            if(currCount != modCount.get()) throw new ConcurrentModificationException("Internal array" +
                    "modified");
            removeAt(actualIdx);
            currCount++;
        }

        /**
         * Checks if an item is present before the current item
         */
        @Override
        public boolean hasPrevious() {
            if(currCount != modCount.get()) throw new ConcurrentModificationException("Internal array" +
                    "modified");
            return actualIdx > 0;
        }

        /**
         * Returns the previous item present before the current item
         */
        @Override
        @SuppressWarnings("unchecked")
        public E previous() {
            if(currCount != modCount.get()) throw new ConcurrentModificationException("Internal array " +
                    "modified");
            if(!hasPrevious()) throw new NoSuchElementException("No more items present");
            E item;
            int relative = actualIdx - (int) (stripeIdx * partition);
            try{
                activeStripe.getReadLock().lock();
                item = (E) activeStripe.elements[relative];
            }finally {
                activeStripe.getReadLock().unlock();
            }
            actualIdx--;
            //Now if possible, shift to next stripe
            if(relative-1 < 0){
                activeStripe = activeStripe.previous;
                stripeIdx--;
            }
            return item;
        }

        /**
         * Sets the current item to the given {@code item}
         */
        @Override
        public void set(E item) {
            if(currCount != modCount.get()) throw new ConcurrentModificationException("Internal array" +
                    "modified");
            ConcurrentArrayList.this.set(actualIdx, item);
            currCount++;
        }
    }


    /**
     * A doubly linked list node that contains a {@link ReentrantReadWriteLock}. This is more efficient
     * implementation compared to a single array, as a growing of array can occur in one stripe. This
     * does increase the overhead to a certain extent but is insignificant enough that it can be
     * compensated for performance.
     *
     */
    private static final class LockedStripes{

        //Lock references for this stripe
        private final ReentrantReadWriteLock stripeLock;

        //Doubly-LinkedList
        public LockedStripes next;
        public LockedStripes previous;

        //A partition determines the absolute max number of elements this stripe could theoretically hold
        private final long partition;

        //Volatile due to multithreaded access
        public volatile Object[] elements;
        private volatile int maxCapacity;
        private volatile int activeCapacity;

        public LockedStripes(long partition, int maxCapacity){
            this.partition = partition;
            this.maxCapacity = maxCapacity;
            next = null;
            previous = null;
            stripeLock = new ReentrantReadWriteLock();
            elements = new Object[maxCapacity];
        }

        /**
         * Checks if the given item is present in this stripe
         */
        public boolean contains(Object item){
            try {
                this.getReadLock().lock();
                for (int i = 0; i < activeCapacity; i++)
                    if (elements[i].equals(item)) return true;
                return false;
            }finally {
                this.getReadLock().unlock();
            }
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

        /**
         * Shifts all the null indices of this stripe to the end and returns the number of nulls
         * Will return -1 if the partition is not fully filled
         */
        public void compressStripe(){
            try {
                this.getWriteLock().lock(); //Making sure this stripe is locked
                int currentPos = 0;
                for (int i = 0; i < activeCapacity; i++)
                    if (elements[i] != null) elements[currentPos++] = elements[i];
                Arrays.fill(elements, currentPos, activeCapacity-1, null);
            }finally {
                this.getWriteLock().unlock();
            }
        }

        /**
         * Reverses the array segment managed by this stripe
         */
        public void reverse(){
            try {
                getWriteLock().lock();
                Object t;
                for (int i = 0, j = activeCapacity - 1; i < j; i++, j--) {
                    t = elements[i];
                    elements[i] = elements[j];
                    elements[j] = t;
                }
            }finally {
                getWriteLock().unlock();
            }
        }
    }
}