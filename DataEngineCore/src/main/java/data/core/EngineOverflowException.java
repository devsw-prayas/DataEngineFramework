package data.core;

/**
 * Thrown when data insertion occurs on completely filled
 * data engine
 *
 * @author Devsw
 */
public class EngineOverflowException extends Exception{
    
	private static final long serialVersionUID = 8777146242623111540L;

	public EngineOverflowException() {
        super("Data Engine Overflow");
    }

    public EngineOverflowException(String message) {
        super(message);
    }
}
