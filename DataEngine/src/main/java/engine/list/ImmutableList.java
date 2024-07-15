package engine.list;

import data.constants.*;
import data.core.*;
import engine.core.AbstractList;
import data.constants.Nature;

import java.util.Iterator;

/**
 * Immutable list extending the fixed size {@code FixedArrayList}. This makes it useful when an
 * unmodifiable, read-only view is required. All the methods that have to be overridden to enable
 * such a behavior is taken care by bytecode-generation at compile time. Generating an immutable-view is
 * recommended through the helper utility class directly.
 *
 * @param <E> Type of data being stored.
 */
@Implementation(ImplementationType.IMPLEMENTATION)
@EngineNature(nature = Nature.IMMUTABLE, behaviour = EngineBehaviour.FIXED_LENGTH,  order = Ordering.UNSUPPORTED)
public class ImmutableList<E> extends FixedArrayList<E>{

    public ImmutableList(final AbstractList<E> list) throws EngineOverflowException, ImmutableException {
        super(list.getActiveSize());
        super.addAll(list);
    }


    @Override
    public <T extends AbstractList<E>> void retainAll(T list) throws EngineUnderflowException, ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    public <T extends AbstractList<E>> void addAll(T list, int start, int end) throws EngineOverflowException, ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    public <T extends AbstractList<E>> void addAllFrom(T list, int start) throws EngineOverflowException, ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    public <T extends AbstractList<E>> void addAll(T list) throws EngineOverflowException, ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    public <T extends DataEngine<E>> T merge(T list) throws EngineUnderflowException, ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    public <T extends DataEngine<E>> T merge(T list, int start) throws EngineUnderflowException, ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    public <T extends DataEngine<E>> T merge(T list, int start, int end) throws EngineUnderflowException, ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    protected void compress() throws ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    protected void shrink() throws ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    protected void grow() throws ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    public void reverse() throws ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    public void add(E item) throws EngineOverflowException, ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    public void add(int index, E item) throws EngineOverflowException, ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");}

    @Override
    public boolean clear() throws ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    public boolean removeAt(int index) throws ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    public boolean remove(E item) throws ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    public boolean removeAll() throws ImmutableException {
        throw new ImmutableException("Unsupported for IMMUTABLE implementations");
    }

    @Override
    public Iterator<E> iterator(){
        return new ImmutableIterator();
    }

    /**
     * Simple iterator for iterating over the list. No fail-fast behavior or synchronization, since
     * the entire list is immutable.
     */
    public final class ImmutableIterator implements Iterator<E>{

        int currentIndex = 0;
        private final ImmutableList<E> immutableList;

        public ImmutableIterator(){
            this.immutableList = ImmutableList.this;
            this.currentIndex = 0;
        }

        @Override
        public boolean hasNext() {
            return immutableList.elements[currentIndex] != null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            return (E)immutableList.elements[currentIndex++];
        }
    }
}
