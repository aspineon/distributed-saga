package no.ssb.concurrent.futureselector;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static no.ssb.concurrent.futureselector.Utils.launder;

public class FutureSelector<F, C> {

    private final AtomicInteger taskCount = new AtomicInteger(0);
    private final BlockingQueue<Selection> doneQueue = new LinkedBlockingQueue<>();

    public Selection<F, C> select() {
        if (taskCount.get() <= 0) {
            throw new IllegalStateException("Attempting to select future for tasks that were never created with \"newFuture\"");
        }
        Selection<F, C> selected;
        try {
            selected = doneQueue.take(); // safe unchecked assignment
        } catch (InterruptedException e) {
            throw launder(e);
        }
        taskCount.decrementAndGet();
        return selected;
    }

    public boolean pending() {
        return taskCount.get() > 0;
    }

    public int add(SelectableFuture<F> selectableFuture, C control) {
        selectableFuture.registerWithDoneQueueAndMarkSelectableIfDone(doneQueue, new Selection<>(selectableFuture, control));
        return taskCount.incrementAndGet();
    }
}
