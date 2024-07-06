package engine.core;


import data.core.EngineOverflowException;
import data.core.ImmutableException;
import engine.list.ImmutableList;

public class DataEngines {

    //Factory class for generating special data-engines or running algorithms
    private DataEngines(){}

    /**
     * Returns an unmodifiable, read-only view of a list by packing it into an {@code ImmutableList}. This
     * method only works on lists, i.e. classes that implement a list by extending {@code AbstractList}
     *
     * @param list The list whose immutable view is needed
     * @return Returns a reference to an {@code ImmutableList} containing all the elements of the passed list
     * @throws EngineOverflowException Thrown when the list is empty or null
     */
    public static ImmutableList<?> getImmutableView(AbstractList<?> list) throws EngineOverflowException, ImmutableException {
        return new ImmutableList<>(list);
    }
}
