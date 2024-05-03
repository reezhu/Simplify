import org.junit.Test;
import org.xjcraft.utils.random.RandomGenerator;

public class RandomTest {
    @Test
    public void random() {
        RandomGenerator generator = new RandomGenerator();
        generator.add(5.0, "a");
        generator.add(10.0, "b");
        generator.add(20.0, "c");
        generator.add(80.0, "d");
        generator.add(90.0, "e");
        generator.add(30.0, "f");
        generator.add(40.0, "g");
        generator.remove("d");
        generator.remove("c");
        for (int i = 0; i < 100; i++) {
//            System.out.print(generator.getRandom());
            assert generator.getRandom() != null;
        }
//        System.out.println();


    }
}
