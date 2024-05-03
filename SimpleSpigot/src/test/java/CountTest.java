import org.xjcraft.utils.count.CountCallback;
import org.xjcraft.utils.count.CountController;

public class CountTest {
    public void count() {
        CountController countController = new CountController(3, new CountCallback() {
            @Override
            public void onCount(int number) {
                System.out.println("a:" + number);
            }

            @Override
            public void onStop() {
                System.out.println("a:stop!");
            }

            @Override
            public void onFinish() {
                System.out.println("a:" + "Booom!");
            }
        });
//        new CountController(5, new CountCallback() {
//            @Override
//            public void onCount(int number) {
//                System.out.println("b:"+number);
//                System.out.println("c:"+countController.getNumber());
//
//            }
//
//            @Override
//            public void onFinish() {
//                System.out.println("b:"+"Booom!");
//            }
//        });
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
