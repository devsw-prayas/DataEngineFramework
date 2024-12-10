package engine.abstraction;

import data.constants.ImplementationType;
import data.core.AbstractDataEngine;
import data.core.Implementation;
import data.function.UnaryOperator;
import engine.behavior.Hasher;

import data.core.ImmutableException;

import java.util.Objects;

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
     *
     */
    public abstract boolean add(E element);

    /**
     * Adds all the elements present in the given set. Duplicates are ignored
     * @param set The set to be added from
     * @return Returns true if any additions have occurred.
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public <T extends AbstractSet<E>> boolean addAll(T set){
        return addAll(0, set.getActiveSize(), set);
    }

    /**
     * Adds all the elements present in the given set lying beyond {@code start}
     * @param start Starting index
     * @param set The set to be added from
     * @return Returns true if any additions occurred
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public <T extends AbstractSet<E>> boolean addAll(int start, T set){
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
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public <T extends AbstractSet<E>> boolean addAll(int start, int end, T set){
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
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public <T extends AbstractSet<E>> boolean removeAll(T set){
        return removeAll(0, set.getActiveSize(), set);
    }

    /**
     * Removes all the elements common to the given set and lying beyond {@code start}
     * @param start Starting index
     * @param set The set to be checked for
     * @return Returns true if any removals occurred, false otherwise.
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public <T extends AbstractSet<E>> boolean removeAll(int start, T set){
        return removeAll(start, set.getActiveSize(), set);
    }

    /**
     * Removes all the elements common to the given set and lying between {@code start} and {@code start}
     * @param start Starting index
     * @param end Ending index
     * @param set The set to be checked for
     * @return Returns true if any removals occurred, false otherwise
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     *
     * @throws UnsupportedOperationException The default implementation behavior is undefined. Other methods that
     * depend on this method will throw it as well. A concrete implementation is required
     */
    public <T extends AbstractSet<E>> boolean removeAll(int start, int end, T set){
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
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public <T extends AbstractSet<E>> boolean containsAll(T set){
        return containsAll(set, 0, set.getActiveSize());
    }

    /**
     * Checks if all the elements present in the given set lying beyond {@code start} are
     * present in the invoking set
     * @param set The set to be checked for
     * @param start Starting index
     * @return Returns true if all elements are present, false otherwise
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public <T extends AbstractSet<E>> boolean containsAll(T set, int start){
        return containsAll(set, start, set.getActiveSize());
    }

    /**
     * Checks if all the elements present in the given set lying in the range {@code start} and
     * {@code end} are present in the invoking set
     * @param set The set to be checked for
     * @param start Starting index
     * @param end Ending index
     * @return Returns true if all elements are present, false otherwise
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public <T extends AbstractSet<E>> boolean containsAll(T set, int start, int end){
        throw new UnsupportedOperationException("Unsupported operation");
    }

    public void replaceAll(UnaryOperator<E> operator){
        Objects.requireNonNull(operator);
        for (E item: this) operator.perform(item);
    }

    //Set operations

    /**
     * The union operation defines a set that contains all the elements present in both
     * the given and invoking set. If the sets are {@link #isDisjoint} then it returns a set containing
     * all the elements of both sets. This is equivalent to the mathematical definition of a set.
     *
     * @param set The set to be considered
     * @return Returns the union of the invoking and passed set
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public abstract <T extends AbstractSet<E>> T union(T set);

    /**
     * The intersection operation defines a set that contains all the elements common to both the invoking
     * and the passed set. If the sets are {@link #isDisjoint} then it returns the constant defined by
     * {@link NullSet}. This is equivalent to the mathematical definition of a set.
     *
     * @param set The set to be considered
     * @return Returns the intersection of the invoking and the passed set
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public abstract <T extends AbstractSet<E>> T intersection(T set);

    /**
     * The difference operation defines a set that contains all the elements that are present only in the
     * invoking set and not the passed set. If both sets are equal then it returns {@link NullSet}. If
     * no common elements are present then it returns the invoking set This is equivalent to the mathematical
     * definition of a set.
     *
     * @param set The set to be considered
     * @return Returns the difference of the invoking and the passed set
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public abstract <T extends AbstractSet<E>> T difference(T set);

    /**
     * The symmetric difference operation defines a set that contains all the elements that are not common to
     * both the sets. If both sets are equal it returns {@link NullSet}. If no common elements are present then
     * it returns the union of both sets. This is equivalent to the mathematical definition of a set.
     *
     * @param set The set to be considered
     * @return Returns the symmetric difference of the invoking and the passed set
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public abstract <T extends AbstractSet<E>> T symmetricDifference(T set);

    /**
     * Two sets are disjoint if they have no common elements. This operation simply checks for disjoint
     * This is equivalent to the mathematical definition of a set.
     *
     * @param set The set to be considered
     * @return Returns true if both are disjoint, false otherwise
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public abstract <T extends AbstractSet<E>> boolean isDisjoint(T set);

    /**
     * A set is considered the subset of another set if all the elements present in it are also
     * present in the other set. A subset rules out the cardinality of both sets. This means two sets
     * that are equal too can be considered a subset of each other.This is equivalent to the
     * mathematical definition of a set.
     *
     * @param set The set to be considered
     * @return Returns true if the invoking set is a subset of the passed set
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public abstract <T extends AbstractSet<E>> boolean isSubset(T set);

    /**
     * Similar to {@link #isSubset}  but checks for cardinality as well. A proper subset always has a
     * cardinality less than that of the set of which it is considered a subset. This is equivalent to the
     * mathematical definition of a set.
     *
     * @param set The set to be considered
     * @return Returns true if the invoking set is a proper subset of the passed set
     *
     * @throws ImmutableException Thrown when the set is {@link NullSet}, or immutable
     */
    public <T extends AbstractSet<E>> boolean isProperSubset(T set){
        return isSubset(set) && this.getActiveSize() < set.getActiveSize();
    }
}
