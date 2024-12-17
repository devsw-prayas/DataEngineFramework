package engine.abstraction;

import data.constants.ImplementationType;
import data.constants.Type;
import data.core.*;

/**
 * Top level class for all graph implementations. Many features are not declared in this class due to
 * the specialized nature of graphs. It is recommended to not alter the abstraction to add support for
 * any feature that may stay dormant in multiple implementations.
 *
 * @param <E> Type argument of edge
 * @param <N> Type argument of node
 *
 * @author Devsw
 */
@Implementation(value = ImplementationType.ABSTRACTION)
public abstract class AbstractGraph<E, N> extends AbstractDataEngine<E> {

    protected int edgeCount;
    protected int nodeCount;

    private AbstractGraph(int dummy){
        super(dummy);
    }

    public AbstractGraph(){
        this(0);
        this.edgeCount = 0;
        this.nodeCount = 0;
    }

    //Active size and max capacity have no meaning for graphs.
    @Override
    @Behaviour(Type.UNSUPPORTED)
    public int getActiveSize() {
        throw new UnsupportedOperationException("Not supported. Use 'getNodeCount' and 'getEdgeCount' ");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    public int getMaxCapacity() {
        throw new UnsupportedOperationException("Not supported. Use 'getNodeCount' and 'getEdgeCount' ");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    protected void setActiveSize(int activeSize) {
        throw new UnsupportedOperationException("Not supported. Use 'getNodeCount' and 'getEdgeCount' ");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    protected void setMaxCapacity(int maxCapacity) {
        throw new UnsupportedOperationException("Not supported. Use 'getNodeCount' and 'getEdgeCount' ");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    public <T extends DataEngine<E>> T merge(T de, int start, int end) throws ImmutableException {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    public <T extends DataEngine<E>> T merge(T de, int start) throws ImmutableException {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    public E[] toArray(int start, int end) {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    public E[] toArray(int start) {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    @Behaviour(Type.UNSUPPORTED)
    public <T extends DataEngine<E>> boolean equals(T de, int start, int end) {
        throw new UnsupportedOperationException("Method not supported");
    }

    /**
     * Adds the given {@code node} into the invoking graph
     * @param node Node to be added
     */
    public abstract void addNode(N node);

    /**
     * Removes the given {@code node} from the invoking graph if present.
     * @param node Node to be removed.
     * @return Returns true if removed, false otherwise
     */
    public abstract boolean removeNode(N node);

    /**
     * Checks if the invoking graph contains the given {@code node}
     * @param node The node to be checked for
     * @return Returns true if the node is present, false otherwise.
     */
    public abstract boolean containsNode(N node);

    /**
     * Creates a returns an immutable-view Data-Engine of all the nodes present in the invoking graph
     * @return Returns an immutable view of all nodes
     * @param <T> The returned Data-Engine
     */
    public abstract <T extends DataEngine<N>> T getAllNodes();

    /**
     * Adds the given {@code edge} between the two nodes {@code node1} and {@code node2}
     * @param edge The edge to be added
     * @param node1 Start point
     * @param node2 End point
     * @return Returns true if edge can be added, false otherwise
     */
    public abstract boolean addEdge(E edge, N node1, N node2);

    /**
     * Removes all the edges lying between {@code node1} and {@code node2}
     * @param node1 Start point
     * @param node2 End point
     * @return Returns true if any edges are removed, false otherwise
     */
    public abstract boolean removeEdge(N node1, N node2);

    /**
     * Creates and returns an immutable-view Data-Engine of all the edges present in the invoking graph
     * @return Returns an immutable view of all edges
     * @param <T> The returned Data-Engine
     */
    public abstract <T extends DataEngine<N>> T getEdges();

    /**
     * @return Returns the count of nodes present in the invoking graph
     */
    public abstract int getNodeCount();

    /**
     * @return Returns the count of edges present in the invoking graph
     */
    public abstract int getEdgeCount();
}
