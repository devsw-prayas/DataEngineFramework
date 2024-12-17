package engine.abstraction;

import data.constants.ImplementationType;
import data.core.DataEngine;
import data.core.ImmutableException;
import data.core.Implementation;
import data.core.AbstractDataEngine;
import data.function.BiFunction;
import data.function.Function;

/**
 * Top level superclass for all matrix implementations. This abstraction builds off of {@link DataEngine}
 * instead of the normal {@link AbstractDataEngine}. This is because a matrix is a special case of a data
 * structure that cannot offer behavior even remotely similar to AbstractDataEngine.
 * @param <E> The type of data that the matrix will store, generally of numeric nature
 */
@Implementation(value = ImplementationType.ABSTRACTION, isSpecialized = true)
public abstract class AbstractMatrix<E extends Number> implements DataEngine<E> {

    protected int rows;
    protected int columns;

    public AbstractMatrix(int rows, int columns){
        this.rows = rows;
        this.columns = columns;
    }
    @Override
    public E[] toArray() {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    public E[] toArray(int start) {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    public E[] toArray(int start, int end) {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    public int getActiveSize() {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    public int getMaxCapacity() {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    public boolean removeAll() throws ImmutableException {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    public <T extends DataEngine<E>> boolean equals(T de) {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    public <T extends DataEngine<E>> boolean equals(T de, int start, int end) {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    public <T extends DataEngine<E>> boolean equivalence(T de) {
        throw new UnsupportedOperationException("Method not supported");
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public boolean isSquare(){
        return rows == columns;
    }

    /**
     * Gets the element at the given {@code row} and {@code column}
     * @param row THe row position
     * @param column The column position
     * @return Returns the item at the given position
     *
     * @throws IllegalArgumentException Thrown when invalid indices are provided
     */
    public abstract E get(int row, int column);

    /**
     * Sets the element at the given {@code row} and {@code column} to the given {@code value}
     * @param row The row position
     * @param column The column position
     * @param value The value to set
     *
     * @throws IllegalArgumentException Thrown when invalid indices are provided
     */
    public abstract void set(int row, int column, E value);

    /**
     * Adds the given {@code matrix} to the invoking matrix using the given {@code adder} function
     * for individual elements
     * @param matrix The matrix to add
     * @param adder The addition operation
     * @return Returns the resulting matrix
     * @param <T> The type of matrix
     *
     * @throws IllegalArgumentException Thrown when the matrices are not conformable
     * @throws NullPointerException Thrown when a {@code null} adder is provided
     */
    public abstract <T extends AbstractMatrix<E>> T add(T matrix, BiFunction<E, E, E> adder);

    /**
     * Subtracts the given {@code matrix} from the invoking matrix using the given {@code subtractor} function
     * for individual elements
     * @param matrix The matrix to subtract
     * @param subtractor The subtraction operation
     * @return Returns the resulting matrix
     * @param <T> The type of matrix
     *
     * @throws IllegalArgumentException Thrown when the matrices are not conformable
     * @throws NullPointerException Thrown when a {@code null} subtractor is provided
     */
    public abstract <T extends AbstractMatrix<E>> T subtract(T matrix, BiFunction<E, E, E> subtractor);

    /**
     * Multiplies the invoking matrix by the given {@code matrix} using the given {@code multiplier} and {@code adder}
     * functions for individual elements
     * @param matrix The matrix to multiply
     * @param multiplier The multiplication operation
     * @param adder The addition operation
     * @return Returns the resulting matrix
     * @param <T> The type of matrix
     *
     * @throws IllegalArgumentException Thrown when the matrices are not conformable
     * @throws NullPointerException Thrown when a {@code null} multiplier or adder is provided
     */
    public abstract <T extends AbstractMatrix<E>> T multiply(T matrix, BiFunction<E, E, E> multiplier, BiFunction<E, E, E> adder);

    /**
     * Multiplies the invoking matrix by the given {@code scalar} using the given {@code multiplier} function
     * for individual elements
     * @param scalar The scalar to multiply by
     * @param multiplier The multiplication operation
     * @return Returns the resulting matrix
     * @param <T> The type of matrix
     *
     * @throws NullPointerException Thrown when a {@code null} multiplier is provided
     */
    public abstract <T extends AbstractMatrix<E>> T multiply(E scalar, BiFunction<E, E, E> multiplier);

    /**
     * Divides the invoking matrix by the given {@code scalar} using the given {@code divider} function
     * for individual elements
     * @param scalar The scalar to divide by
     * @param divider The division operation
     * @return Returns the resulting matrix
     * @param <T> The type of matrix
     *
     * @throws NullPointerException Thrown when a {@code null} divider is provided
     */
    public abstract <T extends AbstractMatrix<E>> T divide(E scalar, BiFunction<E, E, E> divider);

    /**
     * Transposes the invoking matrix
     * @return Returns the transposed matrix
     * @param <T> The type of matrix
     */
    public abstract <T extends AbstractMatrix<E>> T transpose();

    /**
     * Gets the row at the given {@code row} index
     * @param row The row index
     * @return Returns the row
     * @param <T> The type of list containing the row
     */
    public abstract <T extends AbstractList<E>> T getRow(int row);

    /**
     * Gets the column at the given {@code column} index
     * @param column The column index
     * @return Returns the column
     * @param <T> The type of list containing the column
     */
    public abstract <T extends AbstractList<E>> T getColumn(int column);

    /**
     * Applies the given {@code transformation} to the invoking matrix
     * @param transformation The transformation to apply
     * @return Returns the transformed matrix
     * @param <T> The type of matrix
     */
    public abstract <T extends AbstractMatrix<E>> T apply(Function<E, E> transformation);

    /**
     * Calculates the rank of the invoking matrix
     * @return Returns the rank of the matrix
     */
    public E getRank(){
        throw new UnsupportedOperationException("Unsupported operation");
    }

    /**
     * Generates the echelon form of the invoking matrix
     * @return Returns the echelon form of the matrix
     * @param <T> The type of matrix
     */
    public <T extends AbstractMatrix<E>> T getEchelon(){
        throw new UnsupportedOperationException("Unsupported operation");
    }

    /**
     * @return Returns true if the matrix is an identity, false otherwise
     */
    public abstract boolean isIdentity();
}