import engine.implementation.ConcurrentArrayList;

public class Test {
    public static void main(String[] args) {
        ConcurrentArrayList<Integer> list = new ConcurrentArrayList<>();
        for (int i = 0; i < 100000; i++) {
            list.add(i);
            if(i == 2900)
                System.out.println("Bye");
            if(i == 6000)
                System.out.println();
            if(i == 99999)
                System.out.println();
        }
    }
}
