import engine.implementation.ConcurrentArrayList;
import jbenchmark.core.HarnessExecutor;
import jbenchmark.core.Options;

public class CALTests {
    public static void main(String[] args) {
        Options executorOptions = Options.createOption();
        executorOptions.disableOutputToFile();
        ConcurrentArrayList<Integer> list = new ConcurrentArrayList<>();

    }
}
