import org.junit.Test;
import org.xjcraft.utils.StringUtil;


public class StringTest {
//    @Test
//    public void getDirectCoordTest() {
////        String in = "summon zombie ~2 ~2 ~3";
//        getCoordsTest("summon zombie ~2 ~2 ~3");
//        getCoordsTest("mm mobs spawn mob1 1 world,~-1,~2,~-3");
//    }


//    public void getCoordsTest(String in) {
//
//
//        System.out.println(in);
//        String out = StringUtil.getDirectCoord(in, new Location(null, 10, 10, 10));
//        System.out.println(out);
//    }

    @Test
    public void testcncount() {
//        System.out.println(parseCNTime(10));
//        System.out.println(parseCNTime(100));
//        System.out.println(parseCNTime(1000));
//        System.out.println(parseCNTime(10000));
//        System.out.println(parseCNTime(100000));
        for (int i = 0; i < 1000; i++) {
            System.out.println(StringUtil.parseCNTime(i * 10));
        }
    }


}
