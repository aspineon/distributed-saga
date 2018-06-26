package no.ssb.concurrent.futureselector;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SimpleFuture<V> implements Future<V> {

    private final AtomicReference<ReferenceWrapper<Thread>> workerRef = new AtomicReference<>();
    private final AtomicReference<ReferenceWrapper<V>> resultRef = new AtomicReference<>();
    private final AtomicReference<ReferenceWrapper<Throwable>> executionExceptionCauseRef = new AtomicReference<>();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final CountDownLatch doneSignal = new CountDownLatch(1);

    SimpleFuture<V> markDone() {
        doneSignal.countDown();
        return this;
    }

    SimpleFuture<V> clearWorker() {
        workerRef.set(null);
        return this;
    }

    SimpleFuture<V> worker(Thread worker) {
        if (!workerRef.compareAndSet(null, new ReferenceWrapper<>(worker))) {
            throw new IllegalStateException("Worker thread was already set.");
        }
        return this;
    }

    public SimpleFuture<V> complete(V result) {
        if (!resultRef.compareAndSet(null, new ReferenceWrapper<>(result))) {
            throw new IllegalStateException("Task was already completed with previous result.");
        }
        return markDone();
    }

    public SimpleFuture<V> executionException(Throwable cause) {
        if (!executionExceptionCauseRef.compareAndSet(null, new ReferenceWrapper(cause))) {
            throw new IllegalStateException("Execution-exception was already set");
        }
        return markDone();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (resultRef.get() != null) {
            return false;
        }
        if (executionExceptionCauseRef.get() != null) {
            return false;
        }
        if (cancelled.compareAndSet(false, true)) {
            if (mayInterruptIfRunning
                    && workerRef.get() != null
                    && workerRef.get().value != null
                    && !workerRef.get().value.isInterrupted()) {
                workerRef.get().value.interrupt();
            }
            markDone();
        }
        return true;
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public boolean isDone() {
        return resultRef.get() != null || executionExceptionCauseRef.get() != null || cancelled.get();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        doneSignal.await();
        if (resultRef.get() != null) {
            return resultRef.get().value;
        }
        throwExecutionOrCancellationExceptionIfAppropriate();
        throw new IllegalStateException("Done signal set, but task is not done.");
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!doneSignal.await(timeout, unit)) {
            throw new TimeoutException();
        }
        if (resultRef.get() != null) {
            return resultRef.get().value;
        }
        throwExecutionOrCancellationExceptionIfAppropriate();
        throw new IllegalStateException("Done signal set, but task is not done.");
    }

    private void throwExecutionOrCancellationExceptionIfAppropriate() throws ExecutionException {
        if (executionExceptionCauseRef.get() != null) {
            throw new ExecutionException(executionExceptionCauseRef.get().value);
        }
        if (cancelled.get()) {
            throw new CancellationException();
        }
    }

    static class ReferenceWrapper<R> {
        private final R value;

        private ReferenceWrapper(R value) {
            this.value = value;
        }
    }
}
