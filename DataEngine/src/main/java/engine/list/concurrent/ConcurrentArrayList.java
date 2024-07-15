package engine.list.concurrent;

import data.constants.*;
import data.core.*;
import engine.core.AbstractList;

import java.util.Arrays;
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

    @Behaviour(Type.IMMUTABLE)
    private double load(){
        return (double) getActiveSize() / getMaxCapacity();
    }

    @Behaviour(Type.MUTABLE)
    private void flushBuffer(){
        
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
        shift(stripedLock, stripedLock.next);
    }

    private void shift(StripedLocks currentStripe, StripedLocks nextStripe){
        if(!(nextStripe == null)){
            //We are sure that we are somewhere in the interior off the array
            try{
                currentStripe.getWriteLock().lock();
                nextStripe.getWriteLock().lock();
                //Now check the number of nulls
                int nulls = 0;
                for(int i = 0; i < currentStripe.elements.length; i++)
                    //We know the partition is complete
                    if(currentStripe.elements[i] == null) nulls++;

                //Performing compression
                currentStripe.compressStripe();
                nextStripe.compressStripe();

                if(nulls > 0){
                    //So get the items from the next stripe
                    for (int i = (int) (partition - 1 - nulls), j = 0; i < partition && j < nulls; i++, j++)
                        currentStripe.elements[i] = nextStripe.elements[j];
                    //Now null the elements and shift
                    for (int i = 0; i < nulls; i++) nextStripe.elements[i] = null;
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
    protected void grow() {
        if(load() < GROWTH_LOAD_FACTOR) return;
        //We need to make sure where the last stripe lies
        StripedLocks lastStripe = this.stripedLock;
        while (lastStripe.next != null) lastStripe = lastStripe.next;
        int newMaxCapacity = (int) Math.floor(getMaxCapacity() * GOLDEN_RATIO);
        int diff = newMaxCapacity - getMaxCapacity();

        //Case 1
        if(lastStripe.maxCapacity < partition){

            //Check if current stripe has enough space
            if(lastStripe.maxCapacity + diff < partition){
                //Perform a "grow"
                try {
                    lastStripe.getWriteLock().lock();
                    lastStripe.elements = Arrays.copyOf(lastStripe.elements, lastStripe.maxCapacity + diff);
                    lastStripe.maxCapacity = lastStripe.maxCapacity + diff;
                }finally {
                    lastStripe.getWriteLock().unlock();
                }
            }else{
                int rem = (int) (lastStripe.maxCapacity + diff - partition);
                //Create a new stripe with the remaining indices
                try{
                    lastStripe.getWriteLock().lock();
                    lastStripe.elements = Arrays.copyOf(lastStripe.elements, (int) partition);
                    lastStripe.maxCapacity = (int) partition;
                    lastStripe.next = new StripedLocks(partition, rem);
                }finally {
                    lastStripe.getWriteLock().unlock();
                }
            }
        }else{
            //Here the stripe is fully filled
            try{
                lastStripe.getWriteLock().lock();
                lastStripe.next = new StripedLocks(partition, diff);
            }finally {
                lastStripe.getWriteLock().unlock();
            }
        }
        setMaxCapacity(newMaxCapacity);
    }

    /**
     * When the {@code activeSize} is less than {@code SHRINK_LOAD_FACTOR * maxCapacity}, for an
     * underlying array it will end up shrinking by {@code maxCapacity}
     */
    @Override
    protected void shrink() {
        if(load() > SHRINK_LOAD_FACTOR) return;

        //Since we are shrinking, and we know that all the null indices are at last
        //The best way to approach would be again from last, removing nulls until the size is
        //equal to newMaxCapacity
        StripedLocks lastStripe = this.stripedLock, temp = null;
        while (lastStripe.next != null) lastStripe = lastStripe.next;
        int newMaxCapacity = (int) (Math.floor((getMaxCapacity() * GOLDEN_RATIO - getMaxCapacity())));

        //Start removing from end
        int removed = getMaxCapacity() - newMaxCapacity;;
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
                    lastStripe.elements = Arrays.copyOf(lastStripe.elements, lastStripe.maxCapacity - removed);
                    removed = 0;
                }finally {
                    lastStripe.getWriteLock().unlock();
                }
            }
        }while (removed != 0);
    }

    /**
     * Adds an element to the last stripe of the list.
     */
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
                for (int i = currentStripe.activeCapacity; i > index - currentSize; i--)
                    currentStripe.elements[i] = currentStripe.elements[i - 1];
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
            //Now add the element
            try{
                currentStripe.getWriteLock().lock();
                currentStripe.elements[index - currentSize] = item;
            }finally {
                currentStripe.getWriteLock().unlock();
            }
            lastStripe.activeCapacity++;
            grow();
        }
    }

    /**
     * Internal helper method, shifts all the elements through the stripes in a recursive manner.s
     */
    private void recursiveShift(StripedLocks currentStripe, StripedLocks lastStripe, int index) {
        if(!currentStripe.equals(lastStripe)){
            try {
                lastStripe.getWriteLock().lock();
                //We need to shift first
                for (int i = lastStripe.activeCapacity; i > 0; i--)
                    lastStripe.elements[i] = lastStripe.elements[i - 1];
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
                for (int i = lastStripe.activeCapacity; i > index - partition; i--)
                    lastStripe.elements[i] = lastStripe.elements[i - 1];
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
    public <T extends AbstractList<E>> void addAllFrom(T list, int start) {
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
        StripedLocks lastStripe = this.stripedLock;
        while (lastStripe.next != null) lastStripe = lastStripe.next;

        int pos = start;
        while(pos < end) {
            //We obviously will begin with last stripe
            try {
                lastStripe.getWriteLock().lock();
                for (int i = lastStripe.activeCapacity; i < lastStripe.maxCapacity; i++) {
                    if (pos < end) {
                        lastStripe.elements[i] = list.get(pos++);
                        lastStripe.activeCapacity++;
                        setActiveSize(getActiveSize() + 1);
                    }
                    if (load() > GROWTH_LOAD_FACTOR) grow(); //Making sure growth occurs
                }
            } finally {
                lastStripe.getWriteLock().unlock();
            }
            //Now if the stripe is filled to up
            if(lastStripe.activeCapacity == partition && pos < end)
                //Get next stripe, continue until it is finished
                lastStripe = lastStripe.next;
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
    public <T extends AbstractList<E>> boolean containsAll(T list){
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
    public <T extends AbstractList<E>> boolean containsAllFrom(T list, int start){
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
    @Behaviour(Type.MUTABLE)
    public boolean clear() throws ImmutableException {
        return removeAll();
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
     * Generates an {@code AtomicReference} of the data engine. Useful when the object has to be
     * used in a thread blocking context without making the data engine asynchronous.
     *
     * @return Returns an {@code AtomicReference} of the invoking data engine
     * @throws UnsupportedOperationException Will throw an exception when the invoking data engine is
     * already {@code Thread-Mutable}
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
    public <T extends DataEngine<E>> T merge(T de, int start) throws EngineUnderflowException, ImmutableException {
        return super.merge(de, start);
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
    public <T extends DataEngine<E>> T merge(T de, int start, int end) throws EngineUnderflowException, ImmutableException {
        return super.merge(de, start, end);
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
    public E[] toArray(int start) throws EngineUnderflowException {
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
    public E[] toArray(int start, int end) throws EngineUnderflowException {
        return super.toArray(start, end);
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
    public <T extends DataEngine<E>> boolean equals(T de, int start, int end) throws EngineUnderflowException {
        return super.equals(de, start, end);
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
        private final ReentrantReadWriteLock stripeLock;

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
            this.maxCapacity = maxCapacity;
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

        /**
         * Shifts all the null indices of this stripe to the end and returns the number of nulls
         * Will return -1 if the partition is not fully filled
         */
        public int compressStripe(){
            try {
                this.getWriteLock().lock(); //Making sure this stripe is locked
                int currentPos = 0;
                if (maxCapacity < partition) {
                    for (int i = 0; i < activeCapacity; i++)
                        if (elements[i] != null) elements[currentPos++] = elements[i];
                    for (int i = currentPos; i < elements.length; i++) elements[i] = null;

                    return -1;
                } else {
                    int nulls = 0;
                    for (int i = 0; i < elements.length; i++)
                        if (elements[i] != null) elements[currentPos++] = elements[i];
                    for (int i = currentPos; i < elements.length; i++) elements[i] = null;
                    return nulls;
                }
            }finally {
                this.getWriteLock().unlock();
            }
        }
    }
}
