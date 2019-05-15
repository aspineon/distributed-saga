package no.ssb.saga.samples.polyglot;

import io.undertow.Undertow;
import no.ssb.concurrent.futureselector.SelectableThreadPoolExectutor;
import no.ssb.saga.samples.polyglot.sagalog.FileSagaLog;

import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PolyglotMain {

    final SelectableThreadPoolExectutor pool;
    final Undertow server;

    PolyglotMain(int port, String host, String sagaLogPath) {
        AtomicLong nextWorkerId = new AtomicLong(1);
        pool = new SelectableThreadPoolExectutor(
                5, 100,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("sec-worker" + nextWorkerId.getAndIncrement());
                    thread.setUncaughtExceptionHandler((t, e) -> {
                        System.err.println("Uncaught exception in thread " + thread.getName());
                        e.printStackTrace();
                    });
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );

        /*
         * Create a new file-based saga-log. Typically this will be replaced
         * with integration to an external highly-available log with low-latency.
         */
        FileSagaLog sagaLog = new FileSagaLog(Paths.get(sagaLogPath));

        server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(new PolyglotHttpHandler(pool, sagaLog))
                .build();
    }

    private void shutdownPool() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    PolyglotMain start() {
        server.start();
        System.out.format("Server started, listening on %s:%d%n",
                ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getHostString(),
                ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort());
        return this;
    }

    void stop() {
        server.stop();
        shutdownPool();
        System.out.println("Server shut down");
    }

    public static void main(String[] args) {
        PolyglotMain polyglotMain = new PolyglotMain(8139, "127.0.0.1", "./sagalog.dat");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> polyglotMain.stop()));
        polyglotMain.start();
    }
}
