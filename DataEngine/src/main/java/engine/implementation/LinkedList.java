package engine.implementation;

import data.constants.*;
import data.core.*;
import data.function.UnaryOperator;
import engine.abstraction.AbstractList;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
 * as it provides a custom sorting algorithm internally. The algorithm provided internally is defined by
 * {@link LinkedMergeSort}.
 * <p>
 * Null items are not allowed. All insertion methods
 * will throw {@link NullPointerException} when given {@code null} elements
 *
 * @param <E> Type argument of data
 *
 * @author Devsw
 */
@Implementation(ImplementationType.IMPLEMENTATION)
@EngineNature(behaviour = EngineBehaviour.DYNAMIC, nature = Nature.MUTABLE, order = Ordering.UNSORTED)
public class LinkedList<E> extends AbstractList<E> implements Sortable {

    //This implementation ignores the presence of max capacity and instead only uses active size
    public LinkedList() {
        super(0);
        head = new LinkedNode<>();
        tail = head;
    }

    public LinkedList(int size) {
        super(size);
    }

    public LinkedList(AbstractList<E> list) {
        super(list.getMaxCapacity());
        for(E item : list){
            add(item);
        }
    }

    public LinkedList(AbstractList<E> list, int start, int end) {
        super(end - start);
        int block = 0;
        for(E item : list){
            if(block++ < start) continue;
            if(block <= end) add(item);
        }
    }

    public LinkedList(E[] arr){
        super(arr.length);
        addAll(arr);
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
        LinkedNode<E> node = head;
        int pos = 0;
        while(pos++ < idx)
            node = node.next;
        //Now set the item
        node.set(Objects.requireNonNull(item));
    }

    /**
     * Inserts an item directly at the end of the list
     * @param item Item to be added
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void add(E item) {
        insertTail(item);
    }

    /**
     * Inserts an item at the specified position
     * @param index Position of insertion
     * @param item Item to be inserted
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void add(int index, E item) {
        if(index == 0) insertHead(item);
        else if(index == getActiveSize()- 1) insertTail(item);
        else insert(index, item);
    }

    //Helper methods that add items into the list at the head, between the list and at the tail of the list

    /**
     * Inserts an item into the middle of the linked list
     * @param index Insertion position
     * @param item Item to be inserted
     */
    private void insert(int index, E item) {
        if(index > getActiveSize()) throw new IndexOutOfBoundsException("Invalid index");
        if(getActiveSize() == 0){
            head = new LinkedNode<>();
            tail = head;
            tail.set(item);
            incrementSize();
        } else {
            int pos = 0;
            LinkedNode<E> current = head;
            while (pos < index) {
                current = current.next;
                pos++;
            }
            LinkedNode<E> temp = current.next, newNode = new LinkedNode<>();
            newNode.set(Objects.requireNonNull(item));
            current.next = newNode;
            newNode.next = temp;
            temp.previous = newNode;
            newNode.previous = current;
            incrementSize();
        }
    }

    /**
     * Inserts an item to the head of the linked list
     * @param item Item to be inserted
     */
    private void insertHead(E item) {
        //Insert at head is simple
        LinkedNode<E> newHead = new LinkedNode<>();
        newHead.set(Objects.requireNonNull(item));
        head.previous = newHead;
        newHead.next = head;
        head = newHead;
        incrementSize();
    }

    /**
     * Inserts an item to the tail of the linked list
     * @param item Item to be inserted
     */
    private void insertTail(E item) {
        if(getActiveSize() == 0){
            head = new LinkedNode<>();
            tail = head;
            tail.set(item);
            incrementSize();
        }else {
            //Simple, just like the head
            LinkedNode<E> newTail = new LinkedNode<>();
            newTail.set(Objects.requireNonNull(item));
            tail.next = newTail;
            newTail.previous = tail;
            tail = newTail;
            incrementSize();
        }
    }

    /**
     * Adds all items present in {@code arr} in the range {@code start} to {@code end} inclusive. All the items
     * present must be non-null, or an exception will be thrown. This implementation is slightly different in
     * behavior by iterating directly over the nodes.
     *
     * @param arr   An array containing non-null items to add
     * @param start Start point for adding elements
     * @param end   End point for adding elements
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public void addAll(E[] arr, int start, int end) {
        if(start > end | start < 0  | end < 0)
            throw new IndexOutOfBoundsException("Invalid indices provided");
        else if(end - start > arr.length)
            throw new IndexOutOfBoundsException("Invalid indices provided");
        if(getActiveSize() == 0){
            head = new LinkedNode<>();
            tail = head;
            tail.set(arr[start]);
            start++;
        }
        int pos = 0;
        LinkedNode<E> current = head;
        while(pos++ < start)
            current = current.next;

        //Now begin iteration
        for(int i = start; i <= end; i++){
            current = current.next;
            if(current == null){
                //Add new node
                current = new LinkedNode<>();
                current.set(arr[i]);
            }else current.set(arr[i]);
            incrementSize();
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
        //Direct iterative behavior
        LinkedNode<E> current = head;
        while(current != null){
            current = current.next;
            if(current.item.equals(item))
                return true;
        }
        return false;
    }

    /**
     * Removes the {@code item} if it is present in the list. All possible occurrences are removed
     *
     * @param item The item to bo removed
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public boolean remove(E item) {
        LinkedNode<E> current = head, temp;
        if(!this.contains(Objects.requireNonNull(item))) return false;
        while(current != null){
            if(current.item.equals(item)){
                if(current == head) {
                    //Item at head
                    head = current.next;
                    head.previous.next = null;
                    head.previous = null;
                    decrementSize();
                    break;
                }
                if(current == tail){
                    //Item at tail
                    tail = current.previous;
                    current.next.previous = null;
                    current.next = null;
                    decrementSize();
                    break;
                }
                //Remove node, middle of the list
                current.next.previous = current.previous;
                current.previous.next = current.next;
                decrementSize();
            }
            current = current.next;
        }
        return true;
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
        if(index < 0 | index > getActiveSize()) return false;
        //Now iterate
        LinkedNode<E> current = head;
        if(index == 0) {
            //Item at head
            head = current.next;
            head.previous.next = null;
            head.previous = null;
            decrementSize();
        }
        int pos = 0;
        while(pos++ < index)
            current = current.next;
        if(index == getActiveSize()-1){
            //Item at tail
            tail = current.previous;
            current.next.previous = null;
            current.next = null;
            decrementSize();
        }else {
            //Now remove
            current.next.previous = current.previous;
            current.previous.next = current.next;
            decrementSize();
        }
        return true;
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
        Objects.requireNonNull(item);
        int pos = 0;
        LinkedNode<E> current = head;
        while(pos < getActiveSize()){
            if(current.item.equals(item)){
                return pos;
            }
            current = current.next;
            pos++;
        }
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
        Objects.requireNonNull(item);
        int pos = getActiveSize() -1;
        LinkedNode<E> current = tail;
        while(pos >= 0){
            if(current.item.equals(item)){
                return pos;
            }
            current = current.previous;
            pos--;
        }
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
    public E get(int index) {
        if(index < 0 | index > getActiveSize()) throw new IndexOutOfBoundsException("Invalid index provided");
        LinkedNode<E> current = head;
        int pos = 0;
        while(pos++ < index)
            current = current.next;
        return current.item;
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
        return new LinkedList<>(this, start, end);
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
        if(start > end | start < 0 | end < 0 | end - start > getActiveSize())
            throw new IndexOutOfBoundsException("Invalid range provided");
        LinkedNode<E> current = head;
        int pos = 0;
        while(pos++ < start) current = current.next;
        while(pos++ <= end){
            Objects.requireNonNull(operator).perform(current.item);
            current = current.next;
        }
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
        if(head == null | head == tail)
            return;

        LinkedNode<E> top = head, bottom = tail;
        E temp;
        while(top != bottom && top.next != bottom){
            temp = top.item;
            top.item = bottom.item;
            bottom.item = temp;

            top = top.next;
            bottom = bottom.previous;
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
    @Behaviour(Type.IMMUTABLE)
    @SuppressWarnings("unchecked")
    public E[] toArray() {
        Object[] arr = new Object[getActiveSize()];
        LinkedNode<E> current = head;
        int pos = 0;
        while(pos < getActiveSize()){
            arr[pos++] = current.item;
        }
        return (E[]) arr;
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
    public E[] toArray(int start, int end) {
        if(start > end | start < 0 | end < 0 | end - start > getActiveSize())
            throw new IndexOutOfBoundsException("Invalid range provided");
        int pos = 0;
        LinkedNode<E> current = head;
        Object[] arr = new Object[end - start];

        while(pos++ < start) current = current.next;
        while(pos++ <= end){
            arr[pos] = current.item;
            current = current.next;
        }
        return (E[]) arr;
    }

    /**
     * @return Returns true if the invoking data engine has been emptied, false otherwise
     */
    @Override
    @Behaviour(Type.MUTABLE)
    public boolean removeAll() {
        if(getActiveSize() == 0) return false;
        LinkedNode<E> current = head, temp;
        while(current != null){
            temp = current.next;
            current.next = null;
            current.previous = null;
            current = temp;
        }
        head = null;
        tail = null;
        setActiveSize(0);
        return true;
    }

    /**
     * Checks if the invoking data engine and the data engine passed are truly equal, i.e. positions of all elements
     * are identical
     *
     * @param list The data engine to be compared
     * @return Returns true if both are equals, false otherwise
     * @throws EngineUnderflowException Thrown when either of them is empty, or if both are of
     *                                  different lengths
     */
    @Override
    @Behaviour(Type.IMMUTABLE)
    @SuppressWarnings("unchecked")
    public <T extends DataEngine<E>> boolean equals(T list){
        if(!(list instanceof LinkedList<?>)) return false;
        else if(list.getActiveSize() != getActiveSize()) return false;
        else{
            LinkedNode<E> current = head;
            LinkedNode<E> listCurrent = (LinkedNode<E>) ((LinkedList<?>) list).head;
            while(current != null){
                if(!current.item.equals(listCurrent.item)) return false;
                current = current.next;
                listCurrent = listCurrent.next;
            }
            return true;
        }
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
    @SuppressWarnings("unchecked")
    public <T extends DataEngine<E>> boolean equals(T list, int start, int end) {
        int size = this.getActiveSize();
        int size1 = list.getActiveSize();
        if(!(list instanceof LinkedList<?>))
            throw new IllegalArgumentException("The list passed must be a subclass of AbstractList");
        else if (start > size1 | end > size1 | (end - start + 1) > size1)
            throw new IndexOutOfBoundsException("Invalid range: Range beyond passed list");
        else if (start > size | end > size | (end - start + 1) > size)
            throw new IndexOutOfBoundsException("Invalid range: Range beyond invoking list");
        else if (start < 0 | end < 0)
            throw new IndexOutOfBoundsException("Invalid index passed");
        else{
            LinkedNode<E> current = head;
            LinkedNode<E> listCurrent = (LinkedNode<E>) ((LinkedList<?>) list).head;
            int pos = 0;
            while(pos++ < start){
                current = current.next;
                listCurrent = listCurrent.next;
            }

            while(pos++ <= end){
                if(!current.item.equals(listCurrent.item)) return false;
                current = current.next;
                listCurrent = listCurrent.next;
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
    public <T extends DataEngine<E>> boolean equivalence(T list) {
        if(!(list instanceof AbstractList<?>)) return false;
        return this.containsAll((AbstractList<E>)list);
    }

    /**
     * Uses the custom {@link LinkedMergeSort} to perform sorting
     * @param comparator Custom {@link Comparator} for ordering
     */
    @Override
    @Behaviour(Type.MUTABLE)
    @SuppressWarnings("unchecked")
    public void sort(Comparator<?> comparator) {
        LinkedMergeSort sorter = new LinkedMergeSort((Comparator<? super E>) comparator);
        sorter.sort();
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
    public <T extends DataEngine<E>> T merge(T list, int start, int end) throws ImmutableException {
        if(start > end | start < 0 | end < 0 | end - start > list.getActiveSize())
            throw new IndexOutOfBoundsException("Invalid range provided");
        LinkedList<E> merged = new LinkedList<>(this);
        merged.addAll((AbstractList<E>) list, start, end);
        return (T) merged;
    }

    @Override
    @Behaviour(Type.IMMUTABLE)
    public Iterator<E> iterator() {
        return new LinkedIterator();
    }

    /**
     * Internal helper method, finds the midpoint of the linked list
     */
    private LinkedNode<E> getMid(LinkedNode<E> head) {
        if(head == null) return null;
        LinkedNode<E> slow = head;
        LinkedNode<E> fast = head.next;

        while(fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow;
    }

    /**
     * The underlying node class that controls the pointers to the {@code item}, {@code next} and {@code previous}
     * This implementation has no thread-safe mechanisms.
     * @param <E> Type argument of data
     */
    private static final class LinkedNode<E>{
        private E item;
        public LinkedNode<E> next, previous;

        public LinkedNode() {
            next = previous = null;
        }

        public void set(E item){
            this.item = Objects.requireNonNull(item);
        }
    }

    /**
     * A mergesort tailored to this implementation. Any calls to sort, direct or indirect will lead to this class.
     * This implementation works directly in place by separating the nodes and merging in natural ordering based
     * on {@link Comparator}
     */
    private final class LinkedMergeSort{
        private final Comparator<? super E> comparator;
        public LinkedMergeSort(Comparator<? super E> comparator) {
            this.comparator = comparator;
        }

        public void sort(){
            head = mergeSort(head);
        }

        private LinkedNode<E> mergeSort(LinkedNode<E> node){
            if(node == null || node.next == null) return node;

            //Split the list
            LinkedNode<E> middle = getMid(node);
            LinkedNode<E> nextOfMid = middle.next;
            middle.next = null;

            //Sort each half
            LinkedNode<E> left = mergeSort(node);
            LinkedNode<E> right = mergeSort(nextOfMid);

            //Merge both
            return sortedMerge(left, right);
        }

        private LinkedNode<E> sortedMerge(LinkedNode<E> left, LinkedNode<E> right){
            if (left == null) return right;
            if (right == null) return left;

            LinkedNode<E> result;

            if(comparator.compare(left.item, right.item) <= 0){
                result = left;
                result.next = sortedMerge(left.next, right);
            }else {
                result = right;
                result.next = sortedMerge(left, right.next);
            }
            return result;
        }
    }

    public final class LinkedIterator implements ListIterator<E>{

        LinkedNode<E> current;
        int pos;
        public LinkedIterator(){
            current = LinkedList.this.head;
            pos = 0;
        }

        /**
         * Returns {@code true} if the iteration has more elements.
         * (In other words, returns {@code true} if {@link #next} would
         * return an element rather than throwing an exception.)
         *
         * @return {@code true} if the iteration has more elements
         */
        @Override
        public boolean hasNext() {
            return current.next != null;
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration
         * @throws NoSuchElementException if the iteration has no more elements
         */
        @Override
        public E next() {
            if(!hasNext()) throw new NoSuchElementException("No elements present");
            else{
                pos++;
                current = current.next;
                return current.item;
            }
        }


        @Override
        public void remove() {
            LinkedList.this.removeAt(pos);
        }

        /**
         * Checks if an item is present before the current item
         */
        @Override
        public boolean hasPrevious() {
            return current.previous != null;
        }

        /**
         * Returns the previous item present before the current item
         */
        @Override
        public E previous() {
            if(!hasPrevious()) throw new NoSuchElementException("No elements present");
            else{
                pos--;
                current = current.previous;
                return current.item;
            }
        }

        /**
         * Sets the current item to the given {@code item}
         * @param item Item the current position has to be updated with
         */
        @Override
        public void set(E item) {
            current.set(item);
        }
    }
}
