package engine.abstraction;

import data.constants.ImplementationType;
import data.constants.Type;
import data.core.*;
import engine.behavior.Hasher;

/**
 * The top level super class for all maps. A map is a collection of key-value pairs. This abstraction
 * defines all the behavior common to all possible implementations of a map. Any concurrent behavior
 * is to be added manually per implementation. Most of the methods of {@link AbstractDataEngine} and
 * {@link DataEngine} are disabled, instead alternative methods are provided.
 *
 * @param <K> Type argument of the key
 * @param <E> Type argument of the value
 */
@Implementation(value = ImplementationType.ABSTRACTION)
public abstract class AbstractMap<K, E> extends AbstractDataEngine<E> {

    private AbstractMap(){
        super(DEFAULT_CAPACITY);
    }

    //Removing support for invalid methods for a map

    @Override
    @Behaviour(Type.UNSUPPORTED)
    protected void compress() throws ImmutableException {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    protected void shrink() throws ImmutableException {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    protected void grow() throws ImmutableException {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    public int getMaxCapacity() {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    protected void setActiveSize(int activeSize) {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    protected void setMaxCapacity(int maxCapacity) {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    public int getActiveSize() {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    public <T extends DataEngine<E>> T merge(T de, int start) throws ImmutableException {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    public <T extends DataEngine<E>> T merge(T de, int start, int end) throws ImmutableException {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    public <T extends DataEngine<E>> boolean equals(T de, int start, int end) {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    public E[] toArray(int start, int end) {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    public E[] toArray(int start) {
        throw new UnsupportedOperationException("Method not supported");
    }

    protected int computeHash(E element){
        return hasher != null ? hasher.hashcode(element) : element.hashCode();
    }

    private Hasher<E> hasher; //Hashing function

    /**
     * Inserts the given element into the map with the given key. If the key already exists, the
     * method throws an {@link IllegalArgumentException}.
     * @param key The key to be inserted
     * @param element The corresponding element to be inserted
     * @return Returns true if the insertion is successful, false otherwise
     *
     * @throws IllegalArgumentException Thrown when duplicate keys are inserted
     *
     */
    public abstract boolean insert(K key, E element);

    /**
     * Removes the element corresponding to the given key from the map and returns the element.
     * If the key does not exist, it returns null.
     * @param key The key to be removed with its corresponding element
     * @return Returns the element removed, null otherwise
     */
    public abstract E remove(K key);

    /**
     * Returns the element corresponding to the given key. If the key does not exist, it returns null.
     * @param key The key to be searched
     * @return Returns the element corresponding to the key
     */
    public abstract E get(K key);

    /**
     * Checks if the invoking map contains the given key.
     * @param key The key to be checked for
     * @return Returns true if present, false otherwise
     */
    public abstract boolean contains(K key);

    /**
     * @return Returns the total size of the map
     */
    public abstract int size();

    /**
     * Returns an {@link AbstractSet} containing all the keys in the invoking map.
     * @return Returns the set of keys
     * @param <T> The type of the set
     */
    public abstract <T extends AbstractSet<K>> T keySet();

    /**
     * Returns an {@link AbstractList} containing all the values in the invoking map.
     * @return Returns the list of values
     * @param <T> The type of the list
     */
    public abstract <T extends AbstractList<E>> T values();

    /**
     * Inserts all the entries present in the given set into the invoking map.
     * @param entries The entries to be inserted
     * @return Returns true if the insertion is successful
     * @param <T> The type of the set
     */
    public abstract <T extends AbstractSet<MapEntry<K, E>>> boolean putAll(T entries);

    /**
     * Inserts all the entries present in the given iterable into the invoking map.
     * @param entries The iterable to be inserted
     * @return Returns true if the insertion is successful
     * @param <T> The type of the iterable
     */
    public abstract <T extends Iterable<MapEntry<K, E>>> boolean putAll(T entries);

    /**
     * Merges the given map with the invoking map. This implementation of merge simply overwrites any duplicates
     * by considering the value from the invoking map. In case if values are identical then no changes occurs.
     * @param map The map to be merged with
     * @return Returns the merged map
     * @param <T> The type of the map
     */
    public abstract <T extends AbstractMap<K, E>> T merge(T map);

    /**
     * Map entry class that holds a key-value pair. This class is used to represent the entries in a map.
     * @param <K> The type of the key
     * @param <E> The type of the value
     */
    public static final class MapEntry<K, E> implements Comparable<MapEntry<K, E>>{
        private final K key;
        private final E value;

        public MapEntry(K key, E value){
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public E getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "MapEntry{" + "key=" + key + ", value=" + value + '}';
        }

        @Override
        @SuppressWarnings("unchecked")
        public int compareTo(MapEntry<K, E> o) {
            if(key instanceof Comparable)
                return ((Comparable<K>) key).compareTo(o.key);
            else throw new IllegalArgumentException("Keys are not comparable!");
        }
    }
}
