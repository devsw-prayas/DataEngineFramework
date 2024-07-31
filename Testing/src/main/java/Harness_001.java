import engine.implementation.ConcurrentArrayList;
import jbenchmark.harness.BasicHarness;


public class Harness_001 implements BasicHarness {
    private ConcurrentArrayList<Integer> list;
    @Override
    public void init() {
        list = new ConcurrentArrayList<>();
    }

    @Override
    public void benchmark() {
        for (int i = 0; i < 1000000; i++)
            list.offer(i);
    }

    @Override
    public void teardown() {
        list.clear();
    }
}
