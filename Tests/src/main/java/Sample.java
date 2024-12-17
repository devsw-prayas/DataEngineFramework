import engine.implementation.ConcurrentArrayList;
import engine.implementation.DynamicArrayList;

import java.util.Iterator;

public class Sample {
    public static void main(String[] args) {
        ConcurrentArrayList<Integer> list = new ConcurrentArrayList<>();
        list.add(1);
        list.add(9);
        list.add(135);
        list.add(56);
        list.add(5);
        list.add(4);
        Iterator<Integer> iterator = list.iterator();

        while (iterator.hasNext()){
            System.out.println(iterator.next());
        }
    }
}

