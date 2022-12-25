package var;

import java.io.Serial;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("UNCHECKED_CAST")
public class Nikita extends AtomicInteger implements Comparable<Nikita>, Runnable {
    @Serial
    private final int field1 = 0;
    public final int field2 = 0;
    private int[][][] field4 = new int[1][2][4];
    protected BigInteger field3 = BigInteger.ONE;

    @Deprecated
    public static String method1(int param1, double param2, int[][][] arr, BigInteger x) {
        System.out.println(param1);
        System.out.println(param2);
        return Integer.toString(param1) + Double.toString(param2);
    }

    private void method2(int param1) {
        field3 = BigInteger.valueOf(param1);
    }

    @Override
    public int compareTo(Nikita nikita) {
        return 0;
    }

    @Override
    public void run() {

    }
}
