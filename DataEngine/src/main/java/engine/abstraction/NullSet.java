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
 * Many methods of {@link AbstractSet} and its subclasses are required to return NUll set
 */
@Implementation(ImplementationType.IMPLEMENTATION)
@EngineNature(nature =  Nature.IMMUTABLE, order = Ordering.UNSUPPORTED, behaviour = EngineBehaviour.CONSTANT)
public final class NullSet extends AbstractSet<Object> {

    private NullSet(){
        setActiveSize(0);
    }

    public static final NullSet NULL = new NullSet(); //Singleton definition of a null set.

    @Override
    public boolean add(Object element) {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public boolean remove(Object element) {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public boolean contains(Object element) {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public <T extends AbstractSet<Object>> T union(T set) {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public <T extends AbstractSet<Object>> T intersection(T set) {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public <T extends AbstractSet<Object>> T difference(T set) {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public <T extends AbstractSet<Object>> T symmetricDifference(T set) {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public <T extends AbstractSet<Object>> boolean isDisjoint(T set) {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public <T extends AbstractSet<Object>> boolean isSubset(T set) {
        throw new ImmutableException("NULL SET");
    }

    @Override
    protected void compress() throws ImmutableException {
        throw new ImmutableException("NULL SET");
    }

    @Override
    protected void shrink() throws ImmutableException {
        throw new ImmutableException("NULL SET");
    }

    @Override
    protected void grow() throws ImmutableException {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public void reverse() throws ImmutableException {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public boolean isMutable() {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public boolean isThreadSafe() {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public Object[] toArray() {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public Object[] toArray(int start) {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public Object[] toArray(int start, int end) {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public boolean removeAll() throws ImmutableException {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public <T extends DataEngine<Object>> boolean equals(T de) {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public <T extends DataEngine<Object>> boolean equals(T de, int start, int end) {
        throw new ImmutableException("NULL SET");
    }

    @Override
    public <T extends DataEngine<Object>> boolean equivalence(T de) {
        throw new ImmutableException("NULL SET");
    }
}
