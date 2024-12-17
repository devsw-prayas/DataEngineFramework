package engine.behavior;

/**
 * A special marker interface that indicates that the underlying storage contains weak references and
 * must be handled more carefully when compared to strong reference implementations (all the general
 * implementations). Any methods that require careful handling of weak references are guaranteed to
 * be provided or at least given a formal specification on how to be implemented
 *
 * @see java.lang.ref.WeakReference
 */
public interface WeakEngine {}
