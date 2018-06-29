package no.ssb.concurrent.futureselector;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class SelectableFuture<V> extends SimpleFuture<V> implements RunnableFuture<V> {

    private static class DoneQueueState {
        private final AtomicBoolean signalled = new AtomicBoolean(false);
        private final BlockingQueue<Selection> doneQueue;
        private final Selection selection;

        private DoneQueueState(BlockingQueue<Selection> doneQueue, Selection selection) {
            this.doneQueue = doneQueue;
            this.selection = selection;
        }
    }

    private final Callable<V> wrap;
    private final Collection<DoneQueueState> doneQueues = new CopyOnWriteArrayList<>();

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

    SelectableFuture<V> registerWithDoneQueueAndMarkSelectableIfDone(BlockingQueue<Selection> doneQueue, Selection selection) {
        DoneQueueState state = new DoneQueueState(doneQueue, selection);
        doneQueues.add(state);
        if (isDone()) {
            if (state.signalled.compareAndSet(false, true)) {
                if (!state.doneQueue.offer(selection)) {
                    throw new IllegalStateException("Unable to offer this Future instance to doneQueue.");
                }
            }
        }
        return this;
    }

    @Override
    SimpleFuture<V> markDone() {
        SimpleFuture<V> result = super.markDone();
        for (DoneQueueState state : doneQueues) {
            if (state.signalled.compareAndSet(false, true)) {
                if (!state.doneQueue.offer(state.selection)) {
                    throw new IllegalStateException("Unable to offer this Future instance to doneQueue.");
                }
            }
        }
        return result;
    }
}
