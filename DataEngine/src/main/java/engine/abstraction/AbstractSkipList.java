package engine.abstraction;

import data.constants.ImplementationType;
import data.core.DataEngine;
import data.core.Implementation;

@Implementation(value = ImplementationType.ABSTRACTION)
public abstract class AbstractSkipList<E> implements DataEngine<E> {
}
