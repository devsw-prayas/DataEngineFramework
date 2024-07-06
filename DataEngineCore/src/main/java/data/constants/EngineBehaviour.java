package data.constants;

/**
 * Determines the behaviour of the data engine, whether it is growable
 * or of fixed-length
 *
 * @author devsw
 * @since BleedingEdge-alpha-1 *
 */
public enum EngineBehaviour{
    DYNAMIC, FIXED_LENGTH;

    public String toString(){
        return switch (this){
            case DYNAMIC -> "Dynamic";
            case FIXED_LENGTH -> "Static";
        };
    }
}