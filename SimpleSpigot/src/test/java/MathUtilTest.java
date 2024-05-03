import org.junit.Test;
import org.xjcraft.utils.MathUtil;

public class MathUtilTest {
    @Test
    public void random() {
        for (int i = 0; i < 100; i++) {
            int random = MathUtil.random(0, 9);
            assert (random >= 0 && random <= 9);
        }
    }
}
