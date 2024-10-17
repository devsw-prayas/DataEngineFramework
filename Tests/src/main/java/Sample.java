import engine.implementation.DynamicArrayList;

public class Sample {
    public static void main(String[] args) {
        DynamicArrayList<Integer> list = new DynamicArrayList<>();
        list.add(1); //Add items
        list.add(2);
        list.add(3);
        list.add(4);

        list.set(3, 0); //Set items
        list.remove(2); //Remove all occurrences to 2

        list.toArray(); //Get an array representation
        list.clear(); //Empty it, also removeAll()
    }
}

