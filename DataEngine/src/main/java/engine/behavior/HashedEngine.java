package engine.behavior;

/**
 * A special marker interface that indicates that the underlying storage utilizes a {@link Hasher} to
 * generate hash values. This allows for many algorithms to be implemented that require hashing
 * capabilities to be implemented in a generalized way. Most notably, this allows for the graph
 * based structures {@link engine.abstraction.AbstractGraph} to be implemented efficiently
 */
public interface HashedEngine {}
