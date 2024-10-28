package engine.abstraction;

import data.constants.EngineBehaviour;
import data.constants.ImplementationType;
import data.constants.Nature;
import data.constants.Ordering;
import data.core.DataEngine;
import data.core.EngineNature;
import data.core.ImmutableException;
import data.core.Implementation;

/**
 * A set is called {@code NULL} if it has no elements. This singleton class defines a NULL set
 */
@Implementation(ImplementationType.ABSTRACTION)
@EngineNature(nature =  Nature.IMMUTABLE, order = Ordering.UNSUPPORTED, behaviour = EngineBehaviour.FIXED_LENGTH)
public class NullSet extends AbstractSet<Object> {

    private NullSet(){
        setActiveSize(0);
    }

    public static final NullSet NULL = new NullSet(); //Singleton definition of a null set.

    @Override
    public boolean add(Object element) {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public boolean remove(Object element) {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public boolean contains(Object element) {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public AbstractSet<Object> union(AbstractSet<Object> set) {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public AbstractSet<Object> intersection(AbstractSet<Object> set) {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public AbstractSet<Object> difference(AbstractSet<Object> set) {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public AbstractSet<Object> symmetricDifference(AbstractSet<Object> set) {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public boolean isDisjoint(AbstractSet<Object> set) {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public boolean isSubset(AbstractSet<Object> set) {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    protected void compress() throws ImmutableException {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    protected void shrink() throws ImmutableException {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    protected void grow() throws ImmutableException {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public void reverse() throws ImmutableException {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public boolean isMutable() {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public boolean isThreadSafe() {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public Object[] toArray(int start) {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public Object[] toArray(int start, int end) {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public boolean removeAll() throws ImmutableException {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public <T extends DataEngine<Object>> boolean equals(T de) {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public <T extends DataEngine<Object>> boolean equals(T de, int start, int end) {
        throw new UnsupportedOperationException("NULL SET");
    }

    @Override
    public <T extends DataEngine<Object>> boolean equivalence(T de) {
        throw new UnsupportedOperationException("NULL SET");
    }
}
