package engine.abstraction;

import data.constants.ImplementationType;
import data.core.AbstractDataEngine;
import data.core.Implementation;
import engine.behavior.Hasher;

/**
 * The superclass for all Set based implementations. A Set is a collection of unique objects. This class
 * defines methods and behavior common all implementations of a set. Most of the methods replicate the
 * mathematical operations that can be performed on a set. Null elements are not allowed.
 *
 * @param <E> Type argument of data stored
 *
 * @author Devsw
 */
@Implementation(value = ImplementationType.ABSTRACTION)
public abstract class AbstractSet<E> extends AbstractDataEngine<E> {

    protected Hasher<E> hasher; //To be used by classes that support hashing
    public AbstractSet() {
        super(DEFAULT_CAPACITY);
    }

    public AbstractSet(int capacity) {
        super(capacity);
    }

    /**
     * Adds the given {@code element} into the set.
     * @param element The element to be added
     * @return Returns true if addition was successful, false otherwise
     */
    public abstract boolean add(E element);

    /**
     * Adds all the elements present in the given set. Duplicates are ignored
     * @param set The set to be added from
     * @return Returns true if any additions have occurred.
     */
    public boolean addAll(AbstractSet<E> set){
        return addAll(0, set.getActiveSize(), set);
    }

    /**
     * Adds all the elements present in the given set lying beyond {@code start}
     * @param start Starting index
     * @param set The set to be added from
     * @return Returns true if any additions occurred
     */
    public boolean addAll(int start, AbstractSet<E> set){
        return addAll(start, set.getActiveSize(), set);
    }

    /**
     * Adds all the elements present in the given in the set lying between {@code start} and {@code end}
     * @param start Starting index
     * @param end Ending index
     * @param set The set to be added from
     * @return Returns true if any additions occurred
     *
     * @throws UnsupportedOperationException The default implementation behavior is undefined. Other methods that
     * depend on this method will throw it as well. A concrete implementation is required
     */
    public boolean addAll(int start, int end, AbstractSet<E> set){
        throw new UnsupportedOperationException("Unsupported operation");
    }

    /**
     * Removes the given element if present in the set.
     * @param element Element to be removed
     * @return Returns true if removal occurred
     */
    public abstract boolean remove(E element);

    /**
     * Removes all the elements common to the given set
     * @param set The set to be checked for
     * @return Returns true if any removals occurred, false otherwise.
     */
    public boolean removeAll(AbstractSet<E> set){
        return removeAll(0, set.getActiveSize(), set);
    }

    /**
     * Removes all the elements common to the given set and lying beyond {@code start}
     * @param start Starting index
     * @param set The set to be checked for
     * @return Returns true if any removals occurred, false otherwise.
     */
    public boolean removeAll(int start, AbstractSet<E> set){
        return removeAll(start, set.getActiveSize(), set);
    }

    /**
     * Removes all the elements common to the given set and lying between {@code start} and {@code start}
     * @param start Starting index
     * @param end Ending index
     * @param set The set to be checked for
     * @return Returns true if any removals occurred, false otherwise
     *
     * @throws UnsupportedOperationException The default implementation behavior is undefined. Other methods that
     * depend on this method will throw it as well. A concrete implementation is required
     */
    public boolean removeAll(int start, int end, AbstractSet<E> set){
        throw new UnsupportedOperationException("Unsupported operation");
    }

    /**
     * Checks if the given element is present in the invoking set
     * @param element The element to be checked for
     * @return Returns true if present, false otherwise
     */
    public abstract boolean contains(E element);

    /**
     * Checks if all the elements present in the given set are present in invoking set
     * @param set The set to be checked for
     * @return Returns true if all elements are present
     */
    public boolean containsAll(AbstractSet<E> set){
        return containsAll(set, 0, set.getActiveSize());
    }

    /**
     * Checks if all the elements present in the given set lying beyond {@code start} are
     * present in the invoking set
     * @param set The set to be checked for
     * @param start Starting index
     * @return Returns true if all elements are present, false otherwise
     */
    public boolean containsAll(AbstractSet<E> set, int start){
        return containsAll(set, start, set.getActiveSize());
    }

    /**
     * Checks if all the elements present in the given set lying in the range {@code start} and
     * {@code end} are present in the invoking set
     * @param set The set to be checked for
     * @param start Starting index
     * @param end Ending index
     * @return Returns true if all elements are present, false otherwise
     */
    public boolean containsAll(AbstractSet<E> set, int start, int end){
        throw new UnsupportedOperationException("Unsupported operation");
    }

    //Set operations


    public abstract AbstractSet<E> union(AbstractSet<E> set);

    public abstract AbstractSet<E> intersection(AbstractSet<E> set);

    public abstract AbstractSet<E> difference(AbstractSet<E> set);

    public abstract AbstractSet<E> symmetricDifference(AbstractSet<E> set);

    public abstract boolean isDisjoint(AbstractSet<E> set);

    public abstract boolean isSubset(AbstractSet<E> set);

    public boolean isProperSubset(AbstractSet<E> set){
        return isSubset(set) && this.getActiveSize() < set.getActiveSize();
    }
}
