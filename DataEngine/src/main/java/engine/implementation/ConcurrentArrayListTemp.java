package engine.implementation;

import data.constants.*;
import data.core.*;
import engine.abstraction.AbstractList;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

/**
 * This implementation uses lock-striping with {@code StampedLock}s to allow concurrent access for multiple
 * threads simultaneously. A concurrent alternative to {@code DynamicArrayList}. The iterator for this data-engine
 * can be either a snapshot based unidirectional or bidirectional or a fully concurrent iterator that
 * runs on the actual internal array. Certain methods globally lock the entire array like {@code reverse},
 * {@code grow}, {@code shrink}, {@code toArray}, etc. The additional overhead associated with lock-striping
 * is insignificant compared to the global locking with a single lock considering high-contention scenarios
 *
 * @param <E> Type of data being stored.
 *
 * @author devsw
 * @since BleedingEdge-alpha-1
 */
@Implementation(ImplementationType.IMPLEMENTATION)
@EngineNature(nature = Nature.THREAD_MUTABLE,  behaviour = EngineBehaviour.DYNAMIC, order = Ordering.UNSORTED)
public class ConcurrentArrayListTemp<E> extends AbstractList<E> {

    //Fully multithreaded implementation
    private volatile transient Object[] elements;
    private final StripedLock stripedLock;
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

    public ConcurrentArrayListTemp() {
        this(ConcurrentArrayListTemp.LOCK_STRIPING_PARTITION_2048);
    }

    public ConcurrentArrayListTemp(long partition){
        super(DEFAULT_CAPACITY);
        elements = new Object[super.getMaxCapacity()];
        stripedLock = new StripedLock();
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
    public ConcurrentArrayListTemp(int maxCapacity){
        this(maxCapacity, ConcurrentArrayListTemp.LOCK_STRIPING_PARTITION_2048);
    }

    /**
     * Constructor that creates a new list with given {@code maxCapacity} and the given
     * {@code partition}.
      */
    public ConcurrentArrayListTemp(int maxCapacity, long partition) {
        super(maxCapacity);
        elements = new Object[super.getMaxCapacity()];
        this.partition = partition;

        stripedLock = new StripedLock();
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
    public ConcurrentArrayListTemp(AbstractList<E> list) throws EngineOverflowException, ImmutableException {
        this(list, ConcurrentArrayListTemp.LOCK_STRIPING_PARTITION_2048);
    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code list.maxCapacity() +
     * DEFAULT_CAPACITY} and with the given striping {@code partition} for internal locking
     */
    public ConcurrentArrayListTemp(AbstractList<E> list, long partition) throws EngineOverflowException, ImmutableException {
        super(list.getMaxCapacity());
        addAll(list);
        this.partition = partition;

        stripedLock = new StripedLock();
        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) partition];
        isBufferFlushed = new AtomicBoolean(false);
        activeBufferCapacity = new AtomicInteger(0);
    }

    public ConcurrentArrayListTemp(AbstractList<E> list, int maxCapacity) throws EngineOverflowException, ImmutableException {
        this(list, maxCapacity, ConcurrentArrayListTemp.LOCK_STRIPING_PARTITION_2048);
    }

        /**
         * Constructor to add all the elements of the provided list. Empty lists are not allowed.
         * Creates it with all the elements and max capacity equal to the provided {@code maxCapacity +
         * DEFAULT_CAPACITY} and with the given {@code partition}. Throws an exception when
         * {@code maxCapacity < list.getActiveSize()}
         */
    public ConcurrentArrayListTemp(AbstractList<E> list, int maxCapacity, long partition) throws EngineOverflowException, ImmutableException {
        super(maxCapacity);
        this.partition = partition;
        if(maxCapacity < list.getActiveSize())
            throw new IndexOutOfBoundsException("Invalid capacity, Not enough space.");
        else addAll(list);

        stripedLock = new StripedLock();
        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) partition];
        isBufferFlushed = new AtomicBoolean(false);
        activeBufferCapacity = new AtomicInteger(0);
    }

    /**
     * Constructor to add all the elements of the provided arrays Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * DEFAULT_CAPACITY} and with a default partition
     */
    public ConcurrentArrayListTemp(E[] array){
        this(array, ConcurrentArrayListTemp.LOCK_STRIPING_PARTITION_2048);
    }

    /**
     * Constructor to add all the elements of the provided arrays Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * DEFAULT_CAPACITY} and with the given {@code partition}
     */
    public ConcurrentArrayListTemp(E[] array, long partition) {
        super(array.length + DEFAULT_CAPACITY);
        for(E item : array) add(item);
        stripedLock = new StripedLock();
        this.partition = partition;

        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) this.partition];
        isBufferFlushed = new AtomicBoolean(false);
        activeBufferCapacity = new AtomicInteger(0);
    }

    /**
     * Constructor to add all the elements of the provided array. Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * maxCapacity} and with default partition. Throws an exception when {@code maxCapacity < list.getActiveSize()} Although the
     * object can be used further, it will not contain the elements in {@code array}
     */
    public ConcurrentArrayListTemp(E[] array, int maxCapacity){
        this(array, maxCapacity, ConcurrentArrayListTemp.LOCK_STRIPING_PARTITION_2048);
    }

    /**
     * Constructor to add all the elements of the provided array. Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * maxCapacity} and with the given {@code partition}. Throws an exception when
     * {@code maxCapacity < list.getActiveSize()} Although the object can be used further, it will
     * not contain the elements in {@code array}
     */
    public ConcurrentArrayListTemp(E[] array, int maxCapacity, long partition) {
        super(maxCapacity);
        this.partition = partition;
        if(maxCapacity < array.length)
            throw new IndexOutOfBoundsException("Invalid capacity, not enough space");
        else for(E item : array) this.add(item);

        stripedLock = new StripedLock();
        modCount = new AtomicInteger(0);
        stripeBuffer = new Object[(int) this.partition];
        isBufferFlushed = new AtomicBoolean(false);
        activeBufferCapacity = new AtomicInteger(0);
    }

    @Behaviour(Type.IMMUTABLE)
    private void modify() {
        modCount.incrementAndGet();
    }

    /**
     * Unlike normal {@code add} this method adds an item into a buffer array which gets flushed
     * only when a read or removal methods are called. It is recommended that for batch additions
     * through looping should be performed through this method, for adding entire batches, the
     * method {@code addAll} is recommended. These workarounds allow the overhead for continuous
     * locking and unlocking to be reduced significantly.
     *
     * @param item The item to be added
     */
    @Behaviour(Type.MUTABLE)
    public void addBuffered(E item){
        if(item == null) //Null checks
            throw new NullPointerException("Null elements are not allowed");
        //Simply add an item to the buffer until it is full.
        if(activeBufferCapacity.get() < stripeBuffer.length) {
            stripeBuffer[activeBufferCapacity.getAndIncrement()] = item;
            isBufferFlushed.set(false);
        }
        else{
            flushBuffer();
            stripeBuffer[activeBufferCapacity.get()] = item;
            isBufferFlushed.set(false);
        }
    }

    /**
     * Internal helper method, flushes the internal buffer into the array and clears the buffer
     * This method works by accessing individual stripes and adding elements
     */
    private void flushBuffer(){
        if(!isBufferFlushed.get()){
            //Begin flushing
            StampedLock lockStripe = stripedLock.getStripeLock(getActiveSize());
            int activeStripe = (int) (getActiveSize() / partition), prevStripe = activeStripe;
            long stamp = 0; double load = 0.0;
          try{
              stamp = lockStripe.readLock();
              for(int i = 0; i < activeBufferCapacity.get(); i++){
                  activeStripe = (int)((getActiveSize())/ partition);
                  stripedLock.addNewLock();
                  if(activeStripe > prevStripe){
                      //Unlock current stripe, get next
                      lockStripe.unlockRead(stamp);
                      prevStripe = activeStripe;
                      lockStripe = stripedLock.getStripeLock((int) (activeStripe * partition));
                      stamp = lockStripe.readLock();
                  }
                  //Now add elements
                  elements[getActiveSize()] = stripeBuffer[i];
                  setActiveSize(getActiveSize()+1);
                  load = ((double) getActiveSize() / getMaxCapacity());
                  if (load > GROWTH_LOAD_FACTOR) grow();
                  }
          }finally {
              lockStripe.unlockRead(stamp);
          }
            activeBufferCapacity.set(0);
            isBufferFlushed.set(true);
        }
    }

    /**
     * Adds the {@code item} to the list, performs a growth if possible during additions
     *
     * @param item The item to be added
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void add(E item) {
        if(!isBufferFlushed.get()) flushBuffer();
        StampedLock lockStripe = stripedLock.getStripeLock(getActiveSize()+1);
        long stamp = 0;
        try {
            stamp = lockStripe.writeLock();
            //Obviously there's empty space, no need to worry.
            if (item == null) throw new NullPointerException("Item is null");
            else{
                this.elements[getActiveSize()] = item;
                setActiveSize(getActiveSize() +1);
            }
        }finally {
            lockStripe.unlockWrite(stamp);
            modify();
        }
        grow(); //Check for growth
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
        if(!isBufferFlushed.get()) flushBuffer();
        //Here index must be valid
        if (index < 0 | index > this.getActiveSize())
            throw new IndexOutOfBoundsException("Index out of bounds");
        else if (item == null) throw new NullPointerException("Item is null");
        else {
            StampedLock lockStripe = stripedLock.getStripeLock(index+1);
            long stamp = 0;
            try {
                stamp = lockStripe.writeLock();
                //Shift all the elements
                setActiveSize(getActiveSize()+ 1);
                for (int i = getActiveSize(); i > index; i--)
                    elements[i] = elements[i - 1];
                elements[index] = item;
            }finally {
                lockStripe.unlockWrite(stamp);
                modify();
            }
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
        if(!isBufferFlushed.get()) flushBuffer();
        for (Object element : elements)
            if (element.equals(item)) {
                return true;
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
        if(!isBufferFlushed.get()) flushBuffer();
        if (!contains(item)) return false;
        else {
            int stripeIdx = 0, prevStripe = 0;
            StampedLock lockStripe = stripedLock.getStripeLock(0);
            long stamp = 0;
            try {
                stamp = lockStripe.writeLock();
                for (int i = 0; i < getActiveSize(); i++) {
                    stripeIdx = (int) (i / partition);
                    if (stripeIdx > prevStripe) {
                        prevStripe = stripeIdx;
                        lockStripe.unlockWrite(stamp);
                        lockStripe = stripedLock.getStripeLock(i);
                    }

                    if(elements[i].equals(item)){
                        elements[i] = null;
                        setActiveSize(getActiveSize()-1);
                    }
                }
            }finally {
                lockStripe.unlockWrite(stamp);
            }
            stripedLock.removeRedundantStripes();
            compress();
            shrink(); //Perform a shrink if possible
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
        if(!isBufferFlushed.get()) flushBuffer();
        boolean val = true;
        if (index > getActiveSize() - 1 | index < 0) throw new IndexOutOfBoundsException("Invalid Index");
        else if (elements[index] == null) val = false;
        else {
            StampedLock lockStripe = stripedLock.getStripeLock(index);
            long stamp = 0;
            try {
                stamp = lockStripe.writeLock();
                elements[index] = null;
                setActiveSize(getActiveSize()-1);
            }finally {
                lockStripe.unlockWrite(stamp);
                modify();
            }
        }
        stripedLock.removeRedundantStripes();
        compress();
        shrink(); //If possible
        return val;
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
        if(!isBufferFlushed.get()) flushBuffer();
        StampedLock lockStripe = stripedLock.getStripeLock(0);
        long stamp = 0;
        int index = -1, stripeIdx = 0, prevStripe = 0;
        for (int i = 0; i < getActiveSize(); i++){
            stripeIdx = (int) (i / partition);
            if(prevStripe < stripeIdx){
                lockStripe = stripedLock.getStripeLock((int) (stripeIdx * partition));
                prevStripe = stripeIdx;
            }
            do {
                stamp = lockStripe.tryOptimisticRead();
                if(elements[i].equals(item)) index = i;
            }while (lockStripe.validate(stamp));
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
        if(!isBufferFlushed.get()) flushBuffer();
        StampedLock lockStripe = stripedLock.getStripeLock(0);
        long stamp = 0;
        int index = -1, stripeIdx = 0, prevStripe = 0;
        for (int i = getActiveSize() - 1; i > 0; i--){
            stripeIdx = (int) (i / partition);
            if(prevStripe < stripeIdx){
                lockStripe = stripedLock.getStripeLock((int) (stripeIdx * partition));
                prevStripe = stripeIdx;
            }
            try{
                do {
                    stamp = lockStripe.tryOptimisticRead();
                    if(elements[i].equals(item)) index = i;
                }while (lockStripe.validate(stamp));
            }finally {
                lockStripe.unlockRead(stamp);
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
        if(!isBufferFlushed.get()) flushBuffer();
        if (index > this.getActiveSize() | index < 0)
            throw new IndexOutOfBoundsException("Invalid index");
        StampedLock lockStripe = stripedLock.getStripeLock(index);
        long stamp; E item;
        do{
            stamp = lockStripe.tryOptimisticRead();
            item = (E)elements[index];
        }while (lockStripe.validate(stamp));
        return item;
    }

    /**
     * When invoked on a data engine that implements an underlying array, will shift all the elements
     * to the beginning, i.e. a sparsely populated array can be so adjusted that all the elements
     * get move to the front
     */
    @Override
    @Behaviour(Type.MUTABLE)
    protected void compress() {
        if(!isBufferFlushed.get()) flushBuffer();
        while (stripedLock.isFullyLocked);
        long[] stamps = new long[0];
        try {
            stamps = stripedLock.globalWriteLock();
            int currentPos = 0;
            for (int i = 0; i < elements.length; i++)
                if (elements[i] != null) elements[currentPos++] = elements[i];
            for (int i = currentPos; i < elements.length; i++) elements[i] = null;
        }finally {
            stripedLock.globalWriteUnlock(stamps);
            modify();
        }
    }

    /**
     * When the {@code activeSize} is less than {@code SHRINK_LOAD_FACTOR * maxCapacity}, for an
     * underlying array it will end up shrinking by {@code Math.floor(GOLDEN_RATIO * maxCapacity)}
     */
    @Override
    @Behaviour(Type.MUTABLE)
    protected void shrink() {
        if(!isBufferFlushed.get()) flushBuffer();
        while(stripedLock.isFullyLocked);
        long[] stamps = new long[0];
        try {
            stamps = stripedLock.globalWriteLock();
            //First check if the array is sparsely populated
            if ((getActiveSize() * 1.0) / getMaxCapacity() < SHRINK_LOAD_FACTOR) {
                //Then shrink it
                int newMaxCapacity = (int) (Math.floor((getMaxCapacity() * GOLDEN_RATIO - getMaxCapacity())));
                try {
                    E[] temp = this.toArray();
                    this.elements = Arrays.copyOf(temp, newMaxCapacity);
                } catch (EngineUnderflowException exec) { /*Unreachable code */ }
            }
        }finally {
            stripedLock.globalWriteUnlock(stamps);
            stripedLock.removeRedundantStripes();
            modify();
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
        //Making sure no global lock is active
        while(stripedLock.isFullyLocked);
        long[] stamps = null;
        try {
            stamps = stripedLock.globalWriteLock();
            double loadFactor = (double) getActiveSize() / getMaxCapacity();
            // Check if load factor exceeds the threshold for growth
            if (loadFactor > GROWTH_LOAD_FACTOR) {
                // Calculate new capacity using the golden ratio
                int newMaxCapacity = (int) Math.floor(getMaxCapacity() * GOLDEN_RATIO);
                elements = Arrays.copyOf(elements, newMaxCapacity);
                setMaxCapacity(newMaxCapacity);
            }
        }finally {
            stripedLock.globalWriteUnlock(Objects.requireNonNull(stamps));
            stripedLock.addNewLock();
            modify();
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
        if(!isBufferFlushed.get()) flushBuffer();
        //Wait for no active global locks
        while (stripedLock.isFullyLocked) ;

        //Now quickly lock
        long[] stamps = null;
        try {
            stamps = stripedLock.globalWriteLock();
            int left = 0, right = this.getActiveSize() - 1;
            Object t;
            while (left < right) {
                t = elements[right];
                elements[right] = elements[left];
                elements[left] = t;
                left++;
                right--;
            }
        } finally {
            stripedLock.globalWriteUnlock(Objects.requireNonNull(stamps));
            modify();
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
        if(!isBufferFlushed.get()) flushBuffer();
        //Making sure no global locks are active
        while (stripedLock.isFullyLocked);
        long[] stamps = new long[0]; E[] copy;int pos;
        try {
            stamps = stripedLock.globalReadLock();
            if (elements.length == 0) throw new EngineUnderflowException("List is empty");
            copy = (E[]) new Object[elements.length];
            pos = 0;
            for (Object element : elements)
                if (element != null) copy[pos++] = (E) element;
        }finally {
            stripedLock.globalReadUnlock(stamps);
        }
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
    public E[] toArray(int start) throws EngineUnderflowException {
        if(!isBufferFlushed.get()) flushBuffer();
        if (this.getActiveSize() == 0) throw new EngineUnderflowException("List is empty");
        else if (this.getActiveSize() < start) throw new IndexOutOfBoundsException("Invalid start position");
        //Making sure no global lock is active
        while (stripedLock.isFullyLocked) ;
        E[] copy; long[] stamps = new long[0];
        try {
            stamps = stripedLock.globalReadLock();
            copy = (E[]) new Object[this.getActiveSize() - start];
            for (int i = start - 1; i < this.getActiveSize(); i++)
                copy[i - start + 1] = this.get(i);
        }finally {
            stripedLock.globalReadUnlock(stamps);
        }
        return Arrays.copyOf(copy, getActiveSize() - start);
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
        if(!isBufferFlushed.get()) flushBuffer();
        if (this.getActiveSize() == 0) throw new EngineUnderflowException("list is empty");
        else if (end < start | end > this.getActiveSize() | start > this.getActiveSize() | end < 0 | start < 0)
            throw new IndexOutOfBoundsException("Invalid subarray range");
        //Making sure no global lock is active
        while (stripedLock.isFullyLocked) ;
        E[] copy; long[] stamps = new long[0];
        try {
            stamps  = stripedLock.globalReadLock();
            copy = (E[]) new Object[end - start];
            for (int i = start - 1; i < end; i++)
                copy[i - start + 1] = this.get(i);
        } finally {
            stripedLock.globalReadUnlock(stamps);
        }
        return Arrays.copyOf(copy, end - start);
    }

    /**
     * @return Returns true if the invoking data engine has been emptied, false otherwise
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public boolean removeAll() {
        if(!isBufferFlushed.get()) flushBuffer();
        //Wait for no active global lock
        while(stripedLock.isFullyLocked);
        long[] stamps = null;
        boolean val = true;
        try {
            stamps = stripedLock.globalWriteLock();
            if (elements == null || getActiveSize() == 0) val = false;
            else {
                elements = new Object[getMaxCapacity()];
                setActiveSize(0);
            }
        }finally {
            stripedLock.globalWriteUnlock(Objects.requireNonNull(stamps));
            while (getMaxCapacity() > DEFAULT_CAPACITY) shrink(); //Shrink until size is near DEFAULT_CAPACITY
            modify();
        }
        stripedLock.removeRedundantStripes();
        return val;
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
        if(!isBufferFlushed.get()) flushBuffer();
        if (!(list instanceof AbstractList<?>)) throw new IllegalArgumentException("The provided list " +
                "must be a subclass of AbstractList");
        else if (this.getActiveSize() != list.getActiveSize())
            return false;
        else if (this.getActiveSize() == 0 | list.getActiveSize() == 0)
            throw new EngineUnderflowException("Either of the lists is empty");
        else {
            while (stripedLock.isFullyLocked);
            long[] stamps = new long[0];
            try {
                stamps = stripedLock.globalReadLock();
                for (int i = 0; i < this.getActiveSize(); i++) {
                    if (this.get(i) != ((AbstractList<?>) list).get(i))
                        return false;
                }
            }finally {
                stripedLock.globalReadUnlock(stamps);
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
    public <T extends DataEngine<E>> boolean equals(T list, int start, int end) {
        if(!isBufferFlushed.get()) flushBuffer();
        int size = this.getActiveSize();
        int size1 = list.getActiveSize();
        boolean val = true;
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
                    if (!this.get(block).equals(item)) val = false;
                }
                block++;
            }
        }
        return val;
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
        if(!isBufferFlushed.get()) flushBuffer();
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
        if(!isBufferFlushed.get()) flushBuffer();
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
                ConcurrentArrayListTemp<E> temp = new ConcurrentArrayListTemp<>(copy1);
                //Now inject the rest
                for (E item : copy2) temp.add(item);
                return (T) (new ConcurrentArrayListTemp<>(temp, this.getMaxCapacity() + list.getMaxCapacity()));
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
    public <T extends DataEngine<E>> T merge(T list, int start) throws EngineUnderflowException, ImmutableException {
        if(!isBufferFlushed.get()) flushBuffer();
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
                ConcurrentArrayListTemp<E> temp = new ConcurrentArrayListTemp<>(copy1);
                //Now inject the rest
                for (E item : copy2) temp.add(item);
                return (T) (new ConcurrentArrayListTemp<>(temp,
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
    public <T extends DataEngine<E>> T merge(T list, int start, int end) throws EngineUnderflowException, ImmutableException {
        if(!isBufferFlushed.get()) flushBuffer();
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
                ConcurrentArrayListTemp<E> temp = new ConcurrentArrayListTemp<>(copy1);
                //Now inject the rest
                for (E item : copy2) temp.add(item);
                return (T) (new ConcurrentArrayListTemp<>(temp,
                        this.getMaxCapacity() + list.getMaxCapacity()));
            } catch (EngineOverflowException exec) { /*This obviously will be never hit */ }
        }
        return null; //Unreachable
    }

    @Override
    @Behaviour(Type.IMMUTABLE)
    public Iterator<E> iterator() {
        try {
            if(!isBufferFlushed.get()) flushBuffer();
            return new SnapshotIterator();
        } catch (EngineUnderflowException e) {
            throw new RuntimeException(e);
        }
    }

    @Behaviour(Type.IMMUTABLE)
    public Iterator<E> concurrentIterator(){
        return new ConcurrentIterator();
    }

    /**
     * Internal lock striping class that allows for more fine-grain control over multithreading
     * Utilizes {@code StampedLock}s for lock striping and due to its optimistic-read behavior.
     * Declared private as it has little to no value beyond the context of this class.
     */
    private final class StripedLock {
        private final DynamicArrayList<StampedLock> locks;

        private boolean isFullyLocked;

        public StripedLock() {
            locks = new DynamicArrayList<>();
            locks.add(new StampedLock());
            isFullyLocked = false;
        }

        /**
         * Simply adds a new {@code StampedLock}
         */
        public void addNewLock() {
            int index = (int) ((ConcurrentArrayListTemp.this.getActiveSize() + 1) / partition);
            int rem  = (int) ((ConcurrentArrayListTemp.this.getActiveSize()+1) % partition);
            //We need to make sure if 1 stripe extra is present if a remainder is non-zero
            if(rem >= 0 && index == locks.getActiveSize())
                //Then add one
                locks.add(new StampedLock());

        }

        /**
         * Redundant locks get removed. The total stripes along with one additional stripe is retained.
         */
        public void removeRedundantStripes(){
            int length = ConcurrentArrayListTemp.this.getActiveSize();
            long striped = length / ConcurrentArrayListTemp.this.partition, stripeDiff = locks.getActiveSize() - striped;
            if(stripeDiff != 0 & length % ConcurrentArrayListTemp.this.partition > 0)
                for (long i = striped; i < locks.getActiveSize(); i++) locks.removeAt((int)i); //Retain stipe
            else if (stripeDiff != 0)
                for (long i = striped-1; i < locks.getActiveSize(); i++) locks.removeAt((int)i);
        }

        /**
         * Returns the lock present at the given stripe-index
         */
        public StampedLock getStripeLock(final int index) {
            int stripe = (int) (index / ConcurrentArrayListTemp.this.partition);
            if(stripe + 1 > locks.getActiveSize())
                addNewLock();
            return locks.get(stripe);
        }

        /**
         * Obtains a full write lock
         */
        public long[] globalWriteLock(){
            long[] stamps = new long[locks.getActiveSize()];
            isFullyLocked = true;
            for (int i = 0; i < locks.getActiveSize(); i++)
                stamps[i] = locks.get(i).writeLock();
            return stamps;
        }

        /**
         * Validates current stamps per stripe
         */
        public boolean validateGlobalLock(long[] stamps){
            for (int i = 0; i < stamps.length; i++)
                if(!locks.get(i).validate(stamps[i]))
                    return false;
            return true;
        }

        /**
         * Unlocks the global write lock
         */
        public void globalWriteUnlock(long[] stamps){
            for (int i = 0; i < stamps.length; i++)
                locks.get(i).unlockWrite(stamps[i]);
            isFullyLocked = false;
        }

        /**
         * Obtains a full read lock
         */
        public long[] globalReadLock(){
            long[] stamps = new long[locks.getActiveSize()];
            isFullyLocked = true;
            for (int i = 0; i < locks.getActiveSize(); i++)
                stamps[i] = locks.get(i).readLock();
            return stamps;
        }

        /**
         * Unlocks the global write lock
         */
        public void globalReadUnlock(long[] stamps){
            for (int i = 0; i < stamps.length; i++)
                locks.get(i).unlockRead(stamps[i]);
            isFullyLocked = false;
        }
    }

    /**
     * A more advanced version of the iterator. The underlying data gets concurrently accessed through
     * lock-stripes, i.e. the underlying array itself. It implements the exact mechanism as implemented in
     * {@code DynamicArrayList}. This fail-fast behavior is also accompanied by bidirectional access through
     * {@code hasPrevious} and {@code previous}
     */
    public final class ConcurrentIterator implements Iterator<E> {

        private int modCount;
        private int currPos;
        private boolean next = false, previous = false;

        public ConcurrentIterator(){
            modCount = ConcurrentArrayListTemp.this.modCount.get();
            currPos = 0;

        }

        @Override
        public boolean hasNext() {
            next = false;
            return ConcurrentArrayListTemp.this.getActiveSize() > currPos;
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            if(modCount == ConcurrentArrayListTemp.this.modCount.get()) {
                next = true;
                E item;
                long stamp = 0;
                StampedLock lockStripe = ConcurrentArrayListTemp.this.stripedLock.getStripeLock(currPos);
                try {
                    do {
                        stamp = lockStripe.tryOptimisticRead();
                        item = (E) elements[currPos];
                    } while (!lockStripe.validate(stamp));
                } finally {
                    lockStripe.unlockRead(stamp);
                    currPos++;
                }
                return item;
            } else{
                //Reset
                modCount = ConcurrentArrayListTemp.this.modCount.get();
                currPos = 0;
                throw new ConcurrentModificationException("Concurrent Modification occurred during iteration");
            }
        }

        public boolean hasPrevious(){
            previous = false;
            return 0 < currPos;
        }

        @SuppressWarnings("unchecked")
        public E previous() {
            if (modCount == ConcurrentArrayListTemp.this.modCount.get()) {
                previous = true;
                E item;
                long stamp = 0;
                StampedLock lockStripe = ConcurrentArrayListTemp.this.stripedLock.getStripeLock(currPos);
                try {
                    do {
                        stamp = lockStripe.tryOptimisticRead();
                        item = (E) elements[currPos];
                    } while (!lockStripe.validate(stamp));
                } finally {
                    lockStripe.unlockRead(stamp);
                    currPos--;
                }
                return item;
            } else{
                //Reset
                modCount = ConcurrentArrayListTemp.this.modCount.get();
                currPos = 0;
                throw new ConcurrentModificationException("Concurrent Modification occurred during iteration");
            }
        }
    }

    /**
     * A concrete implementation of {@code Iterator} for {@code ConcurrentArrayList}. Uses a copy-on-write
     * mechanism for uninterrupted iterative behaviour. A new copy is generated every time a new iterator is
     * generated, it will contain a snapshot of the underlying array at that moment. The iterator is
     * bidirectional. For concurrent behavior use {@code ConcurrentIterator}
     */
    public final class SnapshotIterator implements Iterator<E> {

        //Internal Array
        private final E[] arraySnapshot;
        int currPos;

        public SnapshotIterator() throws EngineUnderflowException {
            currPos = 0;
            arraySnapshot = ConcurrentArrayListTemp.this.toArray();
        }

        @Override
        public boolean hasNext() {
            return currPos < arraySnapshot.length;
        }

        @Override
        public E next() {
            return arraySnapshot[currPos++];
        }

        public boolean hasPrevious(){
            return currPos > 0;
        }

        public E previous(){
            return arraySnapshot[currPos--];
        }
    }
}