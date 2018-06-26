package no.ssb.concurrent.futureselector;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class SelectableFuture<V> extends SimpleFuture<V> implements RunnableFuture<V> {

    private static class DoneQueueState<R> {
        private final AtomicBoolean signalled = new AtomicBoolean(false);
        private final BlockingQueue<SelectableFuture<R>> doneQueue;

        private DoneQueueState(BlockingQueue<SelectableFuture<R>> doneQueue) {
            this.doneQueue = doneQueue;
        }
    }

    private final Callable<V> wrap;
    private final Collection<DoneQueueState<V>> doneQueues = new CopyOnWriteArrayList<>();

    public SelectableFuture(Runnable runnable, V value) {
        wrap = () -> {
            runnable.run();
            return value;
        };
    }

    public SelectableFuture(Callable<V> wrap) {
        this.wrap = wrap;
    }

    @Override
    public void run() {
        worker(Thread.currentThread());
        try {
            complete(wrap.call());
        } catch (Exception e) {
            executionException(e);
        } finally {
            clearWorker();
        }
    }

    SelectableFuture<V> registerWithDoneQueueAndMarkSelectableIfDone(BlockingQueue<SelectableFuture<V>> doneQueue) {
        DoneQueueState<V> state = new DoneQueueState<>(doneQueue);
        doneQueues.add(state);
        if (isDone()) {
            if (state.signalled.compareAndSet(false, true)) {
                if (!state.doneQueue.offer(this)) {
                    throw new IllegalStateException("Unable to offer this Future instance to doneQueue.");
                }
            }
        }
        return this;
    }

    @Override
    SimpleFuture<V> markDone() {
        SimpleFuture<V> result = super.markDone();
        for (DoneQueueState<V> state : doneQueues) {
            if (state.signalled.compareAndSet(false, true)) {
                if (!state.doneQueue.offer(this)) {
                    throw new IllegalStateException("Unable to offer this Future instance to doneQueue.");
                }
            }
        }
        return result;
    }
}
