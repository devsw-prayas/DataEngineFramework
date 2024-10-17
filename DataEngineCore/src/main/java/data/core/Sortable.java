package data.core;

import java.util.Comparator;

/**
 * A special {@link FunctionalInterface} that must be implemented by data-engines that allow
 * thread-safe behavior. The custom annotation processors will force this to be implemented unless the
 * implementation supports auto-sort based insertion and removals.
 * This is a {@link FunctionalInterface} and is a part of {@code DataEngineFramework}
 *
 * @see EngineNature
 * @see DataEngine
 *
 * @author devsw
 */
@FunctionalInterface
public interface Sortable {
    void sort(Comparator<?> comparator);
}
