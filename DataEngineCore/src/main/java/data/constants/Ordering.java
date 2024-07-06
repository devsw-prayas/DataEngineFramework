package data.constants;

/**
 * Ordering refers to whether the {@code DataEngine} is sorted, or unsorted or if ordering is
 * not supported intrinsically. Immutable forms do not possess an intrinsic ordering. This is taken
 * care of by sorting algorithms or by creating an immutable view directly from a sorted list.
 *
 * @author devsw
 * @since BleedingEdge-alpha-1
 */
public enum Ordering {
    SORTED, UNSORTED, UNSUPPORTED;

    public String toString(){
        return switch (this) {
            case UNSORTED -> "Unsorted";
            case SORTED -> "Sorted";
            case UNSUPPORTED -> "Unsupported";
        };
    }
}
