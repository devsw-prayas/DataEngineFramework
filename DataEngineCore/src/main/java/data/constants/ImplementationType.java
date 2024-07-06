package data.constants;

/**
 * One of the core markers for a {@code DataEngine}, it indicates whether a given class
 * which is part of the {@code DataEngine Framework} acts as an {@code IMPLEMENTATION} or
 * is extendable to create implementation by being declared {@code ABSTRACTION}.
 *
 * @author devsw
 * @since BleedingEdge-alpha-1
 */
public enum ImplementationType {
    ABSTRACTION, IMPLEMENTATION;

    @Override
    public String toString() {
        return switch (this){
            case ABSTRACTION -> "abstraction";
            case IMPLEMENTATION -> "implementation";
        };
    }
}