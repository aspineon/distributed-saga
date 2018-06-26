package no.ssb.concurrent.futureselector;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static no.ssb.concurrent.futureselector.Utils.launder;

public class FutureSelector<V> {

    private final AtomicInteger taskCount = new AtomicInteger(0);
    private final BlockingQueue<SelectableFuture<V>> doneQueue = new LinkedBlockingQueue<>();

    public SelectableFuture<V> select() {
        if (taskCount.get() <= 0) {
            throw new IllegalStateException("Attempting to select future for tasks that were never created with \"newFuture\"");
        }
        SelectableFuture selectableFuture;
        try {
            selectableFuture = doneQueue.take();
        } catch (InterruptedException e) {
            throw launder(e);
        }
        taskCount.decrementAndGet();
        return selectableFuture;
    }

    public boolean pending() {
        return taskCount.get() > 0;
    }

    public int add(SelectableFuture<V> selectableFuture) {
        selectableFuture.registerWithDoneQueueAndMarkSelectableIfDone(doneQueue);
        return taskCount.incrementAndGet();
    }
}
