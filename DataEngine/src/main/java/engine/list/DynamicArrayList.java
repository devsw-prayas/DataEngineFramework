package engine.list;

import data.constants.*;
import data.core.*;
import engine.core.AbstractList;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * A fully dynamic array-list implementation that is not thread-safe. An iterator for this data-engine
 * will throw a {@code ConcurrentModificationException} when the list is altered through any means
 * except the {@code remove} defined by it. Iterator auto-resets. Implementation uses an array of
 * {@code Object}s, to allow a simpler implementation. Null items are not allowed in the list. In
 * case a null object is passed, methods which add to the list will throw {@code NullPointerException}
 *
 * @param <E> Type of data being stored.
 *
 * @author devsw
 * @since BleedingEdge-alpha-1
 */
@Implementation(ImplementationType.IMPLEMENTATION)
@EngineNature(nature = Nature.MUTABLE,  behaviour = EngineBehaviour.DYNAMIC, order = Ordering.UNSORTED)
public class DynamicArrayList<E> extends AbstractList<E> {

    public DynamicArrayList() {
        super(DEFAULT_CAPACITY);
        elements = new Object[this.getMaxCapacity()];
        modCount = 0;
    }

    public DynamicArrayList(int maxCapacity) {
        super(maxCapacity);
        elements = new Object[this.getMaxCapacity()];
        modCount = 0;
    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code list.maxCapacity() +
     * DEFAULT_CAPACITY}
     */
    public DynamicArrayList(AbstractList<E> list) throws EngineOverflowException, ImmutableException {
        super(list.getMaxCapacity());
        addAll(list);
    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code maxCapacity +
     * DEFAULT_CAPACITY}. Throws an exception when {@code maxCapacity < list.getActiveSize()}
     */
    public DynamicArrayList(AbstractList<E> list, int maxCapacity) throws EngineOverflowException, ImmutableException {
        super(maxCapacity);
        if(maxCapacity < list.getActiveSize())
            throw new IndexOutOfBoundsException("Invalid capacity, Not enough space.");
        else addAll(list);
    }

    /**
     * Constructor to add all the elements of the provided arrays Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * DEFAULT_CAPACITY}
     */
    public DynamicArrayList(E[] array) throws EngineOverflowException, ImmutableException {
        super(array.length + DEFAULT_CAPACITY);
        for(E item : array) add(item);
    }

    /**
     * Constructor to add all the elements of the provided array. Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * maxCapacity}. Throws an exception when {@code maxCapacity < list.getActiveSize()}
     */
    public DynamicArrayList(E[] array, int maxCapacity) throws EngineOverflowException, ImmutableException {
        super(maxCapacity);
        if(maxCapacity < array.length)
            throw new IndexOutOfBoundsException("Invalid capacity, not enough space");
        else for(E item : array) this.add(item);
    }

    //Single Threaded Implementation, does not expect concurrent operations
    private transient Object elements[];
    private int modCount;

    private void incrementModification() {
        modCount++;
    }

    /**
     * Adds the {@code item} to the list, performs a growth if possible during additions
     *
     * @param item The item to be added
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void add(E item) {
        //Obviously there's empty space, no need to worry.
        if (item == null) throw new NullPointerException("Item is null");
        else {
            this.elements[getActiveSize()] = item;
            setActiveSize(getActiveSize()+1);
            incrementModification();
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
        else if (item == null) throw new NullPointerException("Item is null");
        else {
            //Perform a "grow" if possible
            grow();

            //Shift all the elements
            setActiveSize(getActiveSize()+1);
            for (int i = getActiveSize(); i > index; i--)
                elements[i] = elements[i - 1];
            elements[index] = item;
            incrementModification();
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
    @SuppressWarnings("unchecked")
    @Behaviour(Type.IMMUTABLE)
    public boolean contains(E item) {
        for (Object element : elements) {
            if (((E) element).equals(item)) {
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
            for (int i = 0; i < getActiveSize(); i++) {
                if (elements[i].equals(item)) {
                    elements[i] = null;
                    incrementModification();
                }
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
            elements[index] = null;
            incrementModification();
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
        for (int i = 0; i < getActiveSize(); i++) if (elements[i].equals(item)) return i;
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
        for (int i = getActiveSize() - 1; i > 0; i--) if (elements[i].equals(item)) return i;
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
        if (index > this.getActiveSize() | index < 0)
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
        double loadFactor = (double) getActiveSize() / getMaxCapacity();
        // Check if load factor is below the shrink factor
        if (loadFactor < SHRINK_LOAD_FACTOR) {
            incrementModification();
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
            incrementModification();
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
        incrementModification();
        int left = 0, right = this.getActiveSize() - 1;
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
    @SuppressWarnings("unchecked")
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
            incrementModification();
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
                DynamicArrayList<E> temp = new DynamicArrayList<>(copy1);
                //Now inject the rest
                for (E item : copy2) temp.add(item);
                return (T) (new DynamicArrayList<>(temp, this.getMaxCapacity() + list.getMaxCapacity()));
            } catch (EngineOverflowException | ImmutableException exec) {
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
    public <T extends DataEngine<E>> T merge(T list, int start) throws EngineUnderflowException, ImmutableException {
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
                DynamicArrayList<E> temp = new DynamicArrayList<>(copy1);
                //Now inject the rest
                for (E item : copy2) temp.add(item);
                return (T) (new DynamicArrayList<>(temp,
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
                DynamicArrayList<E> temp = new DynamicArrayList<>(copy1);
                //Now inject the rest
                for (E item : copy2) temp.add(item);
                return (T) (new DynamicArrayList<>(temp,
                        this.getMaxCapacity() + list.getMaxCapacity()));
            } catch (EngineOverflowException exec) { /*This obviously will be never hit */ }
        }
        return null; //Unreachable
    }

    @Override
    public Iterator<E> iterator() {
        return new DynamicArrayListIterator();
    }

    /**
     * A concrete implementation of {@code Iterator} for {@code DynamicArrayList}. Uses a fail-fast
     * mechanism similar to the {@code Collections Framework}. Will throw {@code ConcurrentModificationException}
     * when alteration occurs while accessing an iterator. The iterator will self reset/
     */
    public final class DynamicArrayListIterator implements Iterator<E> {

        private int currModCount;
        int currPos;
        DynamicArrayList<E> connectedList; //Enclosing List
        boolean next = false;

        public DynamicArrayListIterator() {
            currPos = 0;
            currModCount = DynamicArrayList.this.modCount;
            this.connectedList = DynamicArrayList.this;
        }

        @Override
        public boolean hasNext() {
            next = false;
            return currPos < connectedList.getActiveSize();
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            this.next = true;
            return (E)connectedList.elements[currPos++];
        }

        @Override
        public void remove() {
            if(this.currModCount == connectedList.modCount) {
                connectedList.incrementModification();
                currModCount++;
                //Removing item

                connectedList.elements[currPos--] = null;
                connectedList.compress(); //Perform adjustment
                connectedList.incrementModification();
                currModCount++;

                connectedList.shrink();
                connectedList.incrementModification();
                currModCount++;
                //Simply toggling flag
                next = false;
            }else {
                //Performing an auto reset
                this.currModCount = connectedList.modCount;
                this.currPos = 0;
                next = false;
                throw new ConcurrentModificationException("Alteration occurred iterator access");
            }
        }
    }
}