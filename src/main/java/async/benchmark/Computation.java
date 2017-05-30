package async.benchmark;

import com.twitter.util.Function0;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.infra.Blackhole;
import rx.Observable;

/**
 *
 */
public final class Computation {

    @Param({"1024"})
    public static volatile int param;

    public static int compute() {
        Blackhole.consumeCPU(param);
        return param;
    }

    public static final Function0<Integer> SCALA_ACTION = new Function0<Integer>() {
        @Override
        public Integer apply() {
            return compute();
        }
    };

    public static final Observable.OnSubscribe<Integer> RX_ACTION = subscriber -> {
        subscriber.onNext(compute());
        subscriber.onCompleted();
    };
}
