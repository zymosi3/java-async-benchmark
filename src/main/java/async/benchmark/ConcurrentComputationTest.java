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
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static akka.dispatch.ExecutionContexts.fromExecutor;
import static akka.dispatch.Futures.future;
import static async.benchmark.Computation.SCALA_ACTION;
import static async.benchmark.Computation.RX_ACTION;


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
public class ConcurrentComputationTest {

    private static ExecutorService executor;

    private static Scheduler scheduler;

    private static FuturePool futurePool;

    private static ExecutionContext executionContext;

    @Setup
    public static void initExecutor() {
        executor = Executors.newCachedThreadPool();
        scheduler = Schedulers.from(executor);
        futurePool = new ExecutorServiceFuturePool(executor);
        executionContext = fromExecutor(executor);
    }

    @TearDown
    public static void killExecutor() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(1, TimeUnit.SECONDS))
            executor.shutdownNow();
    }

    @Benchmark
    public static void completableFuture(Blackhole blackhole) throws ExecutionException, InterruptedException {
        CompletableFuture.supplyAsync(Computation::compute, executor).thenAccept(blackhole::consume);
    }

    @Benchmark
    public static void rxJava(Blackhole blackhole) {
        Observable.create(RX_ACTION).subscribeOn(scheduler).first().subscribe(blackhole::consume);
    }

    @Benchmark
    public static void akkaFuture(Blackhole blackhole) throws ExecutionException, InterruptedException {
        future(Computation::compute, executionContext).onComplete(new AbstractFunction1<Try<Integer>, Object>() {
            @Override
            public Object apply(Try<Integer> result) {
                blackhole.consume(result.get());
                return null;
            }
        }, executionContext);
    }

    @Benchmark
    public static void guavaListenableFuture(Blackhole blackhole) throws ExecutionException, InterruptedException {
        final ListenableFuture<Integer> listenableFuture = MoreExecutors.listeningDecorator(executor).submit(Computation::compute);

        com.google.common.util.concurrent.Futures.addCallback(listenableFuture, new FutureCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                blackhole.consume(result);
            }

            @Override
            public void onFailure(Throwable t) {
                throw new RuntimeException(t);
            }
        }, executor);
    }

    @Benchmark
    public static void scalaFuture(Blackhole blackhole) throws ExecutionException, InterruptedException {
        Future.apply(SCALA_ACTION, executionContext).onComplete(new AbstractFunction1<Try<Integer>, Object>() {
            @Override
            public Object apply(Try<Integer> result) {
                blackhole.consume(result.get());
                return null;
            }
        }, executionContext);
    }

    @Benchmark
    public static void finagleFuture(Blackhole blackhole) throws ExecutionException, InterruptedException {
        futurePool.apply(SCALA_ACTION).onSuccess(new AbstractFunction1<Integer, BoxedUnit>() {
            @Override
            public BoxedUnit apply(Integer result) {
                blackhole.consume(result);
                return null;
            }
        });
    }
}
