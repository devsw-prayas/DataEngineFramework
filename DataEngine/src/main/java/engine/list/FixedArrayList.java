package engine.list;

import data.constants.*;
import data.core.*;
import engine.core.AbstractList;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * A fixed size array-list implementation that is not thread-safe. An iterator for this data-engine
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
@EngineNature(nature = Nature.MUTABLE, behaviour = EngineBehaviour.FIXED_LENGTH, order = Ordering.UNSORTED)
public class FixedArrayList<E> extends AbstractList<E> {

    public FixedArrayList() {
        super(AbstractDataEngine.DEFAULT_CAPACITY);
        this.elements = new Object[this.getMaxCapacity()];
    }

    public FixedArrayList(int size) {
        super(size);
        this.elements = new Object[this.getMaxCapacity()];
        this.modCount = 0;
    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code list.maxCapacity() +
     * DEFAULT_CAPACITY}
     */
    public FixedArrayList(AbstractList<E> list) throws EngineOverflowException, ImmutableException {
        super(list.getMaxCapacity());
        addAll(list);
    }

    /**
     * Constructor to add all the elements of the provided list. Empty lists are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code maxCapacity +
     * DEFAULT_CAPACITY}. Throws an exception when {@code maxCapacity < list.getActiveSize()}
     */
    public FixedArrayList(AbstractList<E> list, int maxCapacity) throws EngineOverflowException, ImmutableException {
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
    public FixedArrayList(E[] array) throws EngineOverflowException, ImmutableException {
        super(array.length + DEFAULT_CAPACITY);
        for(E item : array) add(item);
    }

    /**
     * Constructor to add all the elements of the provided array. Empty arrays are not allowed.
     * Creates it with all the elements and max capacity equal to the provided {@code array.length +
     * maxCapacity}. Throws an exception when {@code maxCapacity < list.getActiveSize()}
     */
    public FixedArrayList(E[] array, int maxCapacity) throws EngineOverflowException, ImmutableException {
        super(maxCapacity);
        if(maxCapacity < array.length)
            throw new IndexOutOfBoundsException("Invalid capacity, not enough space");
        else for(E item : array) this.add(item);
    }

    //This a non-concurrent implementation, no need of volatile
    protected transient Object[] elements;
    private transient int modCount;

    /**
     * Moves all the items to the front of the array, pushing all the null spaces to the end
     */
    @Override
    protected void compress() throws ImmutableException {
        incrementModification();
        int insertPos = 0;
        for (int i = 0; i < getActiveSize(); i++)
            if(elements[i] != null) elements[insertPos++] = elements[i];

        for (int i = insertPos; i < getActiveSize(); i++) elements[i] = null;
    }

    //Fixed length implementations, can't grow or shrink
    @Override
    @Behaviour(Type.UNSUPPORTED)
    protected void shrink() throws ImmutableException {
        throw new UnsupportedOperationException("Fixed length implementation");
    }

    //Like I said, fixed length :)
    @Override
    @Behaviour(Type.UNSUPPORTED)
    protected void grow() throws ImmutableException {
        throw new UnsupportedOperationException("Fixed length implementation");
    }

    /**
     * Reverses all the elements present in the array.
     */
    @Behaviour(Type.MUTABLE)
    @Override
    public void reverse() throws ImmutableException {
        incrementModification();
        int left = 0, right = this.getActiveSize() - 1;
        Object t;
        while (left < right) {
            t = elements[right];
            elements[right] = elements[left];
            elements[left] = t;
            left++; right--;
        }
    }

    /**
     * Adds the given {@code item} to the end of the list
     * @param item The item to be added
     * @throws EngineOverflowException Thrown when list is full
     */
    @Behaviour(Type.MUTABLE)
    @Override
    public void add(E item) throws EngineOverflowException, ImmutableException {
        if(item == null) throw new NullPointerException("Item is null");
        if(this.getActiveSize() == this.getMaxCapacity()) {
            throw new EngineOverflowException("Engine Overflow");
        }else{
            this.elements[getActiveSize()] = item;
            setActiveSize(getActiveSize()+1);
            incrementModification();
        }
    }

    /**
     * Adds the given {@code item} at the given {@code index} position
     * @param index The position at which item must be added
     * @param item The item to be added
     * @throws EngineOverflowException Thrown when the list is full
     */
    @Behaviour(Type.MUTABLE)
    @Override
    public void add(int index, E item) throws EngineOverflowException, ImmutableException {
        if(item == null) throw new NullPointerException("Item is null");
        else if(this.getActiveSize() == this.getMaxCapacity()) {
            throw new EngineOverflowException("Engine Overflow");
        }else if(index < 0 | index > this.getActiveSize()) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }else{
            //Shift all elements and add
            setActiveSize(getActiveSize() +1);
            for(int i = getActiveSize(); i > index; i--)
                elements[i] = elements[i-1];
            elements[index] = item;
            incrementModification();
        }
    }

    //Not an efficient implementation, useful only for single-threaded behaviour

    /**
     * Checks if the given {@code item} is present in the list
     * @param item The item to be checked
     * @return Returns true if the item is present, false otherwise
     */
    @Behaviour(Type.IMMUTABLE)
    @Override
    public boolean contains(E item) {
        for(Object element : elements){
            if(element.equals(item)){
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the provided {@code item} from the list, if present.
     * @param item The item to bo removed
     * @return Returns true if removal is successful, false otherwise.
     */
    @Behaviour(Type.MUTABLE)
    @Override
    public boolean remove(E item) throws ImmutableException {
        //This removes all occurrences of item
        if (!contains(item)) return false;
        else {
            for (int i = 0; i < getActiveSize(); i++) {
                if (elements[i].equals(item)) {
                    elements[i] = null;
                    incrementModification();
                }
            }
            compress();
            setActiveSize(getActiveSize()-1);
            return true;
        }
    }

    /**
     * Removes the item at the given {@code index}, if present
     * @param index The position at which an item (if present) is to be removed
     * @return Returns true if removal is successful, false otherwise.
     */
    @Behaviour(Type.MUTABLE)
    @Override
    public boolean removeAt(int index) throws ImmutableException {
        if(index > getActiveSize() -1 | index < 0) throw new IndexOutOfBoundsException("Invalid Index");
        else if(elements[index] == null)
        return false;
        else {
            elements[index] = null;
            incrementModification();
            compress();
            setActiveSize(getActiveSize()-1);
            return true;
        }
    }

    /**
     * Clears the list
     * @return Returns true if cleared, false otherwise
     */
    @Behaviour(Type.MUTABLE)
    @Override
    public boolean clear() throws ImmutableException {
        return removeAll();
    }

    @Override
    @Behaviour(Type.IMMUTABLE)
    public int getFirstIndexOf(E item) {
        for(int i = 0; i < getActiveSize(); i++) if (elements[i].equals(item)) return i;
        return -1;
    }

    @Override
    @Behaviour(Type.IMMUTABLE)
    public int getLastIndexOf(E item) {
        for(int i = getActiveSize() -1; i > 0; i--) if (elements[i].equals(item)) return i;
        return -1;
    }

    /**
     * Returns the element present at the given {@code index}, if valid.
     * @param index The position at which the element is to retrieved
     * @return Returns the element at {@code index}
     */
    @Override
    @SuppressWarnings("unchecked")
    @Behaviour(Type.IMMUTABLE)
    public E get(int index) {
        if(index > this.getActiveSize() | index < 0)
            throw new IndexOutOfBoundsException("Invalid index");
        return (E) elements[index];
    }

    /**
     * Generates a new array that contains all the non-null items of the list
     * @return Returns the array containing all the non-null items
     * @throws EngineUnderflowException Thrown when list is empty
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
     * Removes all the items present in the list, if any present
     * @return Returns true if all items are removed, false otherwise
     */
    @Behaviour(Type.MUTABLE)
    @Override
    public boolean removeAll() throws ImmutableException {
        if(elements == null || getActiveSize() == 0)
            return false;
        else {
            elements = new Object[getMaxCapacity()];
            setActiveSize(0);
            incrementModification();
            return true;
        }
    }

    @Override
    @Behaviour(Type.IMMUTABLE)
    public Iterator<E> iterator(){
        return new FixedArrayListIterator();
    }

    //Internal helper method, updates modCount
    @Behaviour(Type.UNSUPPORTED)
    private void incrementModification(){modCount++;}

    /**
     * This method returns a subarray of the list by starting from {@code start -1} all the
     * way to the end. The start point is inclusive.
     * @param start The starting position for extraction
     * @return Returns an array containing all the elements of the sub-list
     * @throws EngineUnderflowException Thrown when the list is empty
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public E[] toArray(int start) throws EngineUnderflowException {
        return toArray(start, getActiveSize());
    }

    /**
     * This method returns a subarray containing all the elements present in the valid range
     * between {@code start-1} and {@code end-1}. Both end points are inclusive
     * @param start The starting point for extraction
     * @param end The end point of extraction
     * @return Returns a subarray all the elements present in the provided range
     * @throws EngineUnderflowException Thrown when the list is empty
     */
    @Override
    @SuppressWarnings("unchecked")
    @Behaviour(Type.IMMUTABLE)
    public E[] toArray(int start, int end) throws EngineUnderflowException {
        if(this.getActiveSize() == 0) throw new EngineUnderflowException("list is empty");
        else if (end < start | end > this.getActiveSize() | start > this.getActiveSize() | end < 0 | start < 0)
            throw new IndexOutOfBoundsException("Invalid subarray range");
        E[] copy = (E[]) new Object[end-start];
        for (int i = start-1; i < end; i++)
            copy[i- start +1] = this.get(i);
        return Arrays.copyOf(copy, end - start);
    }

    /**
     * Performs a true equality check. Both the length anc complete ordering of elements is done
     * to maintain equality between {@code list} and the invoking list. It does not check for the max
     * capacity.
     * @param list The data engine to be compared with
     * @return Returns true if both are equal, false otherwise
     * @param <T> A subclass of {@code AbstractList}
     * @throws EngineUnderflowException Thrown when either of the lists is empty.
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    public <T extends DataEngine<E>> boolean equals(T list) throws EngineUnderflowException {
        if(!(list instanceof AbstractList<?>)) throw new IllegalArgumentException("The provided list " +
                "must be a subclass of AbstractList");
        else if(this.getActiveSize() != list.getActiveSize())
            return false;
        else if (this.getActiveSize() == 0 | list.getActiveSize() == 0)
            throw new EngineUnderflowException("Either of the lists is empty");
        else {
            for(int i = 0; i < this.getActiveSize(); i++){
                if(this.get(i) != ((AbstractList<?>) list).get(i))
                    return false;
            }
        }
        return true;
    }

    /**
     * Checks if all the items present in both the lists in the range {@code start} to {@code end} (inclusive)
     * are equal, including ordering. This performs strict checking, making it useful for range checks.
     * @param list The list with which equality is to be checked
     * @param start The starting position, inclusive of range
     * @param end The ending position, inclusive of range
     * @return Returns true the range is equal, false otherwise
     * @param <T> A subclass of {@code AbstractList}
     */
    @Override
    @SuppressWarnings("unchecked")
    @Behaviour(Type.IMMUTABLE)
    public <T extends DataEngine<E>> boolean equals(T list, int start, int end) {
        int size = this.getActiveSize();
        int size1 = this.getActiveSize();
        if(!(list instanceof AbstractList<?>))
            throw new IllegalArgumentException("The list passed must be a subclass of AbstractList");
        else if(start > size1 | end > size1 | (end- start + 1) > size1)
                throw new IndexOutOfBoundsException("Invalid range: Range beyond passed list");
        else if(start > size | end > size | (end - start + 1) > size)
                    throw new IndexOutOfBoundsException("Invalid range: Range beyond invoking list");
        else if(start < 0 | end < 0)
                    throw new IndexOutOfBoundsException("Invalid index passed");
        else {
            int block = 0;
            for (E item : (AbstractList<E>)list){
                if(block >= start && block <= end) {
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
     * Checks if both the lists are of the same length and contain the same elements, irrespective of ordering.
     * This is basically an equivalence method. It does not check ordering or frequency of occurrences.
     * Rather an item's presence is sufficient to make it equivalent. For stronger checking, the best way to perform
     * it will be using {@code equals}
     *
     * @param list The list whose equivalence is to be checked
     * @return Returns true if both are equivalent, false otherwise
     * @param <T> A subclass of {@code AbstractList}
     * @throws EngineUnderflowException Thrown when an empty list
     */
    @Override
    @SuppressWarnings("unchecked")
    @Behaviour(Type.IMMUTABLE)
    public <T extends DataEngine<E>> boolean equivalence(T list) throws EngineUnderflowException {
        if(!(list instanceof AbstractList<?>))
            throw new IllegalArgumentException("The passed list must be a subclass of AbstractList");
        else return list.getActiveSize() == this.getActiveSize() & containsAll((AbstractList<E>) list);
    }

    /**
     * Merges the {@code list} provided with the invoking data-engine. Only the items present in the rang
     * {@code start} to {@code end} (inclusive) are merged with the invoking data-engine.
     *
     * @param list  The provided data-engine with which merging is to take place
     * @param start The start point of extraction
     * @param end   The end point of extraction
     * @return Returns the merged data-engine
     */
    @Override
    @Behaviour(Type.MUTABLE)
    @SuppressWarnings("unchecked")
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
                FixedArrayList<E> temp = new FixedArrayList<>(copy1);
                //Now inject the rest
                for (E item : copy2) temp.add(item);
                return (T) (new FixedArrayList<>(temp,
                        this.getMaxCapacity() + list.getMaxCapacity()));
            } catch (EngineOverflowException exec) { /*This obviously will be never hit */ }
        }
        return null; //Unreachable
    }

    /**
     * Merges the {@code list} provided with the invoking data-engine. Only the items present after
     * start (inclusive) are merged with the invoking data-engine.
     *
     * @param list The provided data-engine with which merging is to take place
     * @param start The start point of extraction
     * @return Returns the merged data-engine
     */
    @Override
    @Behaviour(Type.MUTABLE)
    @SuppressWarnings("unchecked")
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
                FixedArrayList<E> temp = new FixedArrayList<>(copy1);
                //Now inject the rest
                for (E item : copy2) temp.add(item);
                return (T) (new FixedArrayList<>(temp,
                        this.getMaxCapacity() + list.getMaxCapacity() - start));
            } catch (EngineOverflowException exec) { /*This obviously will be never hit */ }
        }
        return null; //Unreachable
    }

    /**
     * Merges the {@code list} provided with the invoking list. A new de is generated with
     * max capacity equal to sum of max-capacities of both de containing all the elements
     *
     * @param list The provided data-engine with which merging is to take place
     * @return Returns the merged data-engine
     */
    @Override
    @Behaviour(Type.MUTABLE)
    @SuppressWarnings("unchecked")
    public <T extends DataEngine<E>> T merge(T list) throws EngineUnderflowException, ImmutableException {
        if(!(list instanceof AbstractList<?>))
            throw new IllegalArgumentException("The provided data engine is not a subclass of AbstractList");
        else if(list.getActiveSize() == 0)
            throw new EngineUnderflowException("List is empty");
        else {
            //Generate a copy
            E[] copy1 = list.toArray();
            //Generate another copy
            E[] copy2 = this.toArray();
            //Now generate a new one
            //This method might be inefficient!
            try {
                FixedArrayList<E> temp = new FixedArrayList<>(copy1);
                //Now inject the rest
                for (E item : copy2) temp.add(item);
                return (T) (new FixedArrayList<>(temp, this.getMaxCapacity() + list.getMaxCapacity()));
            } catch (EngineOverflowException exec) { /*This obviously will be never hit */ }
        }
        return null; //Unreachable
    }

    /**
     * A concrete implementation of {@code Iterator} for {@code FixedArrayList}. Uses a fail-fast
     * mechanism similar to the {@code Collections Framework}. Will throw {@code ConcurrentModificationException}
     * when alteration occurs while accessing an iterator. The iterator will self reset/
     */
    public final class FixedArrayListIterator implements Iterator<E> {

        private int currModCount;
        int currPos;
        FixedArrayList<E> connectedList; //Enclosing List
        boolean next = false;

        public FixedArrayListIterator() {
            currPos = 0;
            currModCount = FixedArrayList.this.modCount;
            this.connectedList = FixedArrayList.this;
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
                try {
                    connectedList.compress(); //Perform adjustment
                } catch (ImmutableException e) {
                    throw new RuntimeException(e);
                }
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