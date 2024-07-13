package data.core;

/**
 * Thrown when any data removal or transform operations are called on
 * an empty data engine
 * @author Devsw
 */
public class EngineUnderflowException extends RuntimeException {
  
	private static final long serialVersionUID = -7184207086532541588L;

	public EngineUnderflowException() {
        super("Data Engine Underflow");
    }

    public EngineUnderflowException(String message) {
        super(message);
    }
}
