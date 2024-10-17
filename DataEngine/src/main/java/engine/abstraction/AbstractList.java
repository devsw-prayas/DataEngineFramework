package engine.abstraction;

import data.constants.ImplementationType;
import data.core.*;
import data.constants.Type;
import data.function.UnaryOperator;

import java.util.Iterator;


/**
 * The top-level abstract class for all List implementations, including thread-safe
 * implementations. It defines methods specific to the behaviour of a list, unique value lists are also considered.
 * @param <E>
 *
 * @author Devsw
 */
@Implementation(value = ImplementationType.ABSTRACTION)
public abstract class AbstractList<E> extends AbstractDataEngine<E> {

    public AbstractList(int maxCapacity){
        super(maxCapacity);
    }

    /**
     * Returns the load active on the invoking data-engine
     */
    protected double load(){
        return (getActiveSize() * 1.0 )/ getMaxCapacity();
    }

    /**
     * Sets the item at given {@code idx} (assuming it to be valid) to the given {@code index}
     * @param idx The position where the item is to be changed
     * @param item The item
     */
    public abstract void set(int idx, E item);

    //Methods that will add items to the list

    public abstract void add(E item);

    public abstract void add(int index, E item);

    /**
     * Adds all the items present in {@code list} into the invoking data-engine
     * This method is iterator dependent. Any implementation that is not marked with {@link Implementation}
     * must be careful to have a concrete {@link Iterator} implementation, else the method will throw an error.
     * It is highly recommended to use {@link Implementation} to guarantee that an {@link Iterator} will be
     * present during execution.
     *
     * @param list The list whose items are to be added into the invoking list
     * @param <T> A subclass of {@link AbstractList}
     * @throws IllegalArgumentException Thrown when the list is empty
     * @throws ImmutableException Thrown when it is called on an immutable implementation
     */
    @Behaviour(Type.MUTABLE)
    public <T extends AbstractList<E>> void addAll(T list){
        if(list.getActiveSize() > 0){
            for (E item : list) add(item);
        } else throw new IllegalArgumentException("List is empty");
    }

    /**
     * Similar to the {@code addAll} method, it adds all the items from the {@code start}th index, inclusive.
     * This method is iterator dependent. Any implementation that is not marked with {@link Implementation}
     * must be careful to have a concrete {@link Iterator} implementation, else the method will throw an error.
     * It is highly recommended to use {@link Implementation} to guarantee that an {@link Iterator} will be
     * present during execution.
     *
     * @param list The list whose elements are to be added
     * @param start Starting point for adding elements, inclusive
     * @param <T> A subclass of {@link AbstractList}
     * @throws ImmutableException Thrown when it is called on an immutable implementation
     */
    @Behaviour(Type.MUTABLE)
    public <T extends AbstractList<E>> void addAllFrom(T list, int start){
        if(start > list.getActiveSize())
            throw new IndexOutOfBoundsException("Passed index exceeds number of items present");
        else if(start < 0)
            throw new IndexOutOfBoundsException("Invalid index, index can't be negative: "+start);
        else {
            int blocking = 0;
            for (E item : list){
                if(blocking++ < start); //While we are not at the index, keep blocking input
                else add(item); //After blocking ends, start adding items.
            }
        }
    }


    /**
     *  Adds all the items lying in the range {@code start} to {@code end}. Both endpoints of interval
     *  are inclusive. This method is iterator dependent. Any implementation that is not marked with {@link Implementation}
     *  must be careful to have a concrete {@link Iterator} implementation, else the method will throw an error.
     *  It is highly recommended to use {@link Implementation} to guarantee that an {@link Iterator} will be
     *  present during execution.
     *
     * @param list The list whose elements are to be added
     * @param start Start point for adding elements
     * @param end End point for adding elements
     * @param <T> A subclass of {@link AbstractList}
     * @throws IndexOutOfBoundsException Thrown when endpoints are invalid
     * @throws ImmutableException Thrown when it is called on an immutable implementation
     */
    @Behaviour(Type.MUTABLE)
    public <T extends AbstractList<E>> void addAll(T list, int start, int end){
        if((end - start + 1) > list.getActiveSize())
            throw new IndexOutOfBoundsException("Passed range exceeds number of items present");
        else if(end > list.getActiveSize() | start > list.getActiveSize() |
                start > end | start < 0 | end < 0)
            throw new IndexOutOfBoundsException("Range is invalid");
        else{
            int blocking = 0;
            for (E item : list){
                if(blocking++ < start); //Block until start index is reached
                else if(blocking++ < end) add(item); // Add items until blocking crosses range
            }
        }
    }

    /**
     * Adds all the items present in the given {@code arr}. Non-null elements are not allowed
     * @param arr The array to be added
     * @throws ImmutableException Thrown when it is called on an immutable implementation
     */
    public void addAll(E[] arr){
        addAll(arr, 0, arr.length);
    }

    /**
     * Adds all the items present in the given {@code arr} from {@code start}. Non-null elements are not allowed
     * @param arr The array to be added
     * @param start The start point for adding elements
     * @throws ImmutableException Thrown when it is called on an immutable implementation
     */
    public void addAll(E[] arr, int start){
        addAll(arr, start, arr.length);
    }

    /**
     * Adds all items present in {@code arr} in the range {@code start} to {@code end} inclusive. All the items
     * present must be non-null, or an exception will be thrown. It depends on the {@code add} method. For thread-safe
     * implementations, a more efficient implementation is preferred
     *
     * @param arr An array containing non-null items to add
     * @param start Start point for adding elements
     * @param end End point for adding elements
     * @throws ImmutableException Thrown when it is called on an immutable implementation
     */
    public abstract void addAll(E[] arr, int start, int end);

    /**
     * Checks if an item is present in the invoking list
     * @param item The item to be checked
     * @return Returns true if present, false otherwise
     */
    public abstract boolean contains(E item);

    /**
     * Checks if all the items present in the list are present in the invoking list. This method is iterator dependent.
     * Any implementation that is not marked with {@link Implementation} must be careful to have a concrete {@link Iterator}
     * implementation, else the method will throw an error. It is highly recommended to use {@link Implementation} to
     * guarantee that an {@link Iterator} will be present during execution.
     *
     * @param list The list whose items have to be checked for presence in the invoking list
     * @return Returns true if all items are present, false otherwise
     * @param <T> A subclass of {@link AbstractList}
     * @throws EngineUnderflowException Thrown when sizes of both lists are different.
     */
    @Behaviour(Type.IMMUTABLE)
    public <T extends AbstractList<E>> boolean containsAll(T list){
        if (list.getActiveSize() == 0 | getActiveSize() == 0)
            throw new EngineUnderflowException("Invalid list, a list cannot be empty");
        else{
            for(E item : list){
                if(!contains(item)) return false;
            }
            return true;
        }
    }

    /**
     * Checks if all the items present after the {@code start}th index are present in the invoking list. This method is
     * iterator dependent. Any implementation that is not marked with {@link Implementation} must be careful to have a
     * concrete {@link  Iterator} implementation, else the method will throw an error. It is highly recommended to use
     * {@link  Implementation} to guarantee that an {@link  Iterator} will be present during execution.
     *
     * @param list The list whose elements lying the provided range are to be checked
     * @param start Starting point for checking, inclusive
     * @return Returns true if all the elements present in the range are present in invoking list, false otherwise
     * @param <T> A subclass of {@code AbstractList}
     * @throws EngineUnderflowException Thrown when both lists are of different sizes
     * @throws IndexOutOfBoundsException Thrown when invalid {@code start} index is passed
     */
    @Behaviour(Type.IMMUTABLE)
    public <T extends AbstractList<E>> boolean containsAllFrom(T list, int start){
        if (list.getActiveSize() == 0 | getActiveSize() == 0)
            throw new EngineUnderflowException("Invalid list, a list cannot be empty");
        else if(start > list.getActiveSize() | start < 0)
            throw new IndexOutOfBoundsException("Invalid start point");
        else{
            int blocking = 0;
            for(E item : list){
                if(blocking++ < start) continue; //Force iteration until start index is reached
                if(!contains(item)) return false;
            }
            return true;
        }
    }

    /**
     * Checks if all the items present in the given list in the range {@code start} to {@code end} inclusive is
     * present in the invoking list. This method is iterator dependent. Any implementation that is not marked with
     * {@code Implementation} must be careful to have a concrete {@link Iterator} implementation, else the method will
     * throw an error. It is highly recommended to use {@link Implementation} to guarantee that an {@link Iterator}
     * will be present during execution.
     *
     * @param list The list whose items are to be checked in the specified range.
     * @param start Starting point for check, inclusive.
     * @param end End point for checks, inclusive.
     * @return Returns true if all the items present in then specified range are present in the
     * invoking list, false otherwise.
     * @param <T> A subclass of {@link AbstractList}
     * @throws IndexOutOfBoundsException Thrown when an invalid range is passed.
     */
    @Behaviour(Type.IMMUTABLE)
    public <T extends AbstractList<E>> boolean containsAll(T list, int start, int end){
        if((end - start + 1) > list.getActiveSize())
            throw new IndexOutOfBoundsException("Passed range exceeds number of items present");
        else if(end > list.getActiveSize() | start > list.getActiveSize() | start > end | start < 0 | end < 0)
            throw new IndexOutOfBoundsException("Range is invalid");
        else{
            int blocking = 0;
            for(E item : list){
                if(blocking++ < start) continue;
                if(!contains(item) & blocking < end) return false;
            }
        }
        return true;
    }

    /**
     * Removes the {@code item} if it is present in the list. All possible occurrences are removed
     *
     * @param item The item to bo removed
     * @throws ImmutableException Thrown when it is called on an immutable implementation
     */
    public abstract boolean remove(E item);

    /**
     * Removes the item present at the given index as long as it is not null
     * @param index The position at which an item (if present) is to be removed
     * @return Returns true if an item is removed, false otherwise
     * @throws ImmutableException Thrown when it is called on an immutable implementation
     */
    public abstract boolean removeAt(int index);

    /**
     * Clears all the items in the list.
     * @return Returns true if cleared, false otherwise.
     * @throws ImmutableException Thrown when it is called on an immutable implementation
     */
    public abstract boolean clear();

    /**
     * This method retains all the items that are present in the {@code list} passed as parameter. This method
     * is iterator dependent. Any implementation that is not marked with {@link Implementation} must be careful
     * to have a concrete {@link Iterator} implementation, else the method will throw an error. It is highly
     * recommended to use {@link Implementation} to guarantee that an {@link Iterator} will be present during
     * execution.
     *
     * @param list The list whose items are to be checked
     * @param <T> A subclass of {@link AbstractList}
     * @throws EngineUnderflowException Thrown when an empty list is passed
     * @throws ImmutableException Thrown when it is called on an immutable implementation
     */
    @Behaviour(Type.MUTABLE)
    public <T extends AbstractList<E>> void retainAll(T list){
        if(list.equals(null)) throw new NullPointerException("Null objects are not allowed");
        else if(list.isEmpty()) throw new EngineUnderflowException("List is empty");
        else {
            Iterator<E> iterator = this.iterator(); //Must have an implementation
            while(iterator.hasNext()){
                if(list.contains(iterator.next()));
                else iterator.remove();
            }
        }
    }

    //Position occurrence

    /**
     * Finds the first index of the passed item in the invoking list. Will return -1 if the
     * item is not present in the list
     * @param item The item whose first position is to be calculated
     * @return Returns the index position if present, else -1
     */
    public abstract int getFirstIndexOf(E item);

    /**
     * Finds the last index of the passed item in the invoking list. Will return -1 if the
     * item is not present in the list
     * @param item The item whose last position is to be calculated
     * @return Returns the index if present, else -1
     */
    public abstract int getLastIndexOf(E item);

    /**
     * Returns the item present at the given {@code index}, if valid.
     * @param index The position of retrieval of item
     * @return Returns the item present at the given index.
     */
    public abstract E get(int index);

    /**
     * Creates a list containing all the elements in the range {@code start} to {@code end}.
     * Null indices are not allowed
     * @param start Starting position
     * @param end End position
     * @return Returns the new list
     */
    public abstract AbstractList<E> subList(int start, int end)  ;

    /**
     * Creates a list containing all the elements in the range {@code start} to {@code getActiveSize()}
     * @param start Starting position
     * @return Returns the new list
     */
    @Behaviour(Type.IMMUTABLE)
    public AbstractList<E> subList(int start){
        return subList(start, getActiveSize());
    }

    /**
     * Modifies and replaces all the items present in the list by passing them into an {@link UnaryOperator}
     * @param operator An {@link UnaryOperator} that is applied on all the elements in the list.
     * @throws ImmutableException Thrown when the list is immutable
     */
    @Behaviour(Type.MUTABLE)
    public void replaceAll(UnaryOperator<E> operator)  {
        replaceAll(operator, 0, getActiveSize());
    }

    /**
     * Performs the operation defined by {@code operator} on all the items lying in the range {@code start}
     * to {@code end}. The actual elements in the underlying array or structure are directly modified
     * @param operator An {@link UnaryOperator} that is applied on all the elements present in the range
     * @param start Starting index
     * @param end End index
     * @throws ImmutableException Thrown when the implementation is immutable.
     */
    public abstract void replaceAll(UnaryOperator<E> operator, int start, int end)  ;
}
