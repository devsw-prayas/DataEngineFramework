import jbenchmark.core.HarnessExecutor;
import jbenchmark.core.Options;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Options options = Options.createOption();
        options.enableInfoLevel().enableThreadInfo().enableTimestamps().disableOutputToFile();
        HarnessExecutor executor = new HarnessExecutor(8, options);
        try {
            executor.submit(new Harness_001()).get();
        }finally{
            executor.shutdown();
        }
    }
}