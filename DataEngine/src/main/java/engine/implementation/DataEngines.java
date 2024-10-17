package engine.implementation;

import data.core.DataEngine;
import data.core.Sortable;

import java.util.Comparator;

//TODO
public class DataEngines {

    //Factory class for generating special data-engines or running algorithms
    private DataEngines(){}

    public static void sort(DataEngine<?> dataEngine, Comparator<?> comparator) {
        if(dataEngine instanceof Sortable) ((Sortable) dataEngine).sort(comparator);
    }
}
