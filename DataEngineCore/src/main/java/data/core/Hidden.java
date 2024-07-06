package data.core;

/**
 * A special marker annotation that prevents the processors from kicking in when
 * applied to a class. Most useful when the class is still under development and
 * errors due to incomplete implementations are not warranted
 */
public @interface Hidden {}
