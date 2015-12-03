package async.benchmark;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.twitter.util.ExecutorServiceFuturePool;
import com.twitter.util.FuturePool;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;
import scala.concurrent.ExecutionContext;
import scala.concurrent.impl.Future;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;
import scala.util.Try;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static akka.dispatch.ExecutionContexts.fromExecutor;
import static akka.dispatch.Futures.future;
import static async.benchmark.Computation.findSumOfRoots;
import static async.benchmark.Computation.SCALA_ACTION;
import static async.benchmark.Computation.RX_ACTION;


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
public class DirectComputationTest {

    private static final ExecutorService EXECUTOR = new DirectExecutorService();

    private static final Scheduler SCHEDULER = Schedulers.from(EXECUTOR);

    private static final FuturePool FUTURE_POOL = new ExecutorServiceFuturePool(EXECUTOR);

    private static final ExecutionContext EXECUTION_CONTEXT = fromExecutor(EXECUTOR);

    @Benchmark
    public static void iterativeComputation(Blackhole blackhole) {
        double root = findSumOfRoots();
        blackhole.consume(root);
    }

    @Benchmark
    public static void completableFuture(Blackhole blackhole) throws ExecutionException, InterruptedException {
        CompletableFuture.supplyAsync(Computation::findSumOfRoots, EXECUTOR).thenAccept(blackhole::consume);
    }

    @Benchmark
    public static void rxJava(Blackhole blackhole) {
        Observable.create(RX_ACTION).subscribeOn(SCHEDULER).first().subscribe(blackhole::consume);
    }

    @Benchmark
    public static void akkaFuture(Blackhole blackhole) throws ExecutionException, InterruptedException {
        future(Computation::findSumOfRoots, EXECUTION_CONTEXT).onComplete(new AbstractFunction1<Try<Double>, Object>() {
            @Override
            public Object apply(Try<Double> result) {
                blackhole.consume(result.get());
                return null;
            }
        }, EXECUTION_CONTEXT);
    }

    @Benchmark
    public static void guavaListenableFuture(Blackhole blackhole) throws ExecutionException, InterruptedException {
        final ListenableFuture<Double> listenableFuture = MoreExecutors.listeningDecorator(EXECUTOR).submit(Computation::findSumOfRoots);

        com.google.common.util.concurrent.Futures.addCallback(listenableFuture, new FutureCallback<Double>() {
            @Override
            public void onSuccess(Double result) {
                blackhole.consume(result);
            }

            @Override
            public void onFailure(Throwable t) {
                throw new RuntimeException(t);
            }
        }, EXECUTOR);
    }

    @Benchmark
    public static void scalaFuture(Blackhole blackhole) throws ExecutionException, InterruptedException {
        Future.apply(SCALA_ACTION, EXECUTION_CONTEXT).onComplete(new AbstractFunction1<Try<Double>, Object>() {
            @Override
            public Object apply(Try<Double> result) {
                blackhole.consume(result);
                return null;
            }
        }, EXECUTION_CONTEXT);
    }

    @Benchmark
    public static void finagleFuture(Blackhole blackhole) throws ExecutionException, InterruptedException {
        FUTURE_POOL.apply(SCALA_ACTION).onSuccess(new AbstractFunction1<Double, BoxedUnit>() {
            @Override
            public BoxedUnit apply(Double result) {
                blackhole.consume(result);
                return null;
            }
        });
    }
}
