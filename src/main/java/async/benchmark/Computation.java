package async.benchmark;

import com.twitter.util.Function0;
import rx.Observable;

import java.util.Random;

/**
 *
 */
public final class Computation {

    private static final Random RANDOM = new Random();

    public static double findSumOfRoots() {
        final int a = RANDOM.nextInt(100),
                c = RANDOM.nextInt(100),
                b = 4 * a * c;
        final double root1 = (b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
        final double root2 = (b - Math.sqrt(b * b - 4 * a * c)) / (2 * a);
        return root1 + root2;
    }

    public static final Function0<Double> SCALA_ACTION = new Function0<Double>() {
        @Override
        public Double apply() {
            return findSumOfRoots();
        }
    };

    public static final Observable.OnSubscribe<Double> RX_ACTION = subscriber -> {
        subscriber.onNext(findSumOfRoots());
        subscriber.onCompleted();
    };
}
