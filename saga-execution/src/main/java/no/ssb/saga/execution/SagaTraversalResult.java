package no.ssb.saga.execution;

import no.ssb.concurrent.futureselector.ExecutionRuntimeException;
import no.ssb.concurrent.futureselector.FutureSelector;
import no.ssb.concurrent.futureselector.InterruptedRuntimeException;
import no.ssb.concurrent.futureselector.SelectableFuture;
import no.ssb.concurrent.futureselector.SimpleFuture;
import no.ssb.concurrent.futureselector.TimeoutRuntimeException;
import no.ssb.saga.api.Saga;
import no.ssb.saga.api.SagaNode;
import no.ssb.saga.execution.adapter.VisitationResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static no.ssb.concurrent.futureselector.Utils.launder;

public class SagaTraversalResult {
    private final Saga saga;
    private final AtomicInteger pendingWalks;
    private final BlockingQueue<SelectableFuture<List<String>>> futureThreadWalk;
    private final ConcurrentHashMap<String, SimpleFuture<SelectableFuture<VisitationResult>>> visitFutureById;

    public SagaTraversalResult(Saga saga, AtomicInteger pendingWalks, BlockingQueue<SelectableFuture<List<String>>> futureThreadWalk, ConcurrentHashMap<String, SimpleFuture<SelectableFuture<VisitationResult>>> visitFutureById) {
        this.saga = saga;
        this.pendingWalks = pendingWalks;
        this.futureThreadWalk = futureThreadWalk;
        this.visitFutureById = visitFutureById;
    }

    public <V> void waitForCompletion(long timeout, TimeUnit unit) {
        complete(timeout, unit, selectableFuture -> {
        }, value -> {
        });
    }

    public void complete(long timeout, TimeUnit unit, Consumer<SelectableFuture<List<String>>> threadWalkConsumer, Consumer<Object> visitConsumer) {
        long startTime = System.currentTimeMillis();
        long durationMs = unit.toMillis(timeout);

        Collection<SelectableFuture<List<String>>> futureWalks =
                completeAllThreadWalks(startTime, durationMs);

        for (SelectableFuture<List<String>> futureWalk : futureWalks) {
            threadWalkConsumer.accept(futureWalk);
        }

        FutureSelector<VisitationResult> visitSelector = getVisitFutureSelector(startTime, durationMs);

        while (visitSelector.pending()) {
            SelectableFuture<VisitationResult> selected = visitSelector.select();
            long remainingMs = remaining(startTime, durationMs);
            VisitationResult value;
            try {
                value = selected.get(remainingMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw launder(e);
            }
            visitConsumer.accept(value.output);
        }
    }

    /**
     * Check the future results of all saga graph walks. These future results are created one
     * per parallel execution. Each parallel execution may walk one or more nodes.
     * <p>
     * After this method has completed normally, it is guaranteed that all nodes were
     * walked, and that their execution was successful.
     *
     * @param startTime  start time as a reference for timeout
     * @param durationMs total number of milliseconds that this operation is allowed to take
     *                   before a TimeoutException should be thrown.
     * @return a collection of all the thread-walks performed during saga-traversal
     * @throws TimeoutRuntimeException     if the time is up before all walk results could be retrieved.
     * @throws InterruptedRuntimeException if the calling thread is interrupted while waiting on walk
     *                                     results to complete.
     * @throws ExecutionRuntimeException   if there were any exceptions thrown from a visit.
     */
    private Collection<SelectableFuture<List<String>>> completeAllThreadWalks(long startTime, long durationMs) {
        Collection<SelectableFuture<List<String>>> result = new ArrayList<>();

        FutureSelector<List<String>> walkSelector = new FutureSelector<>();

        result.addAll(drainAllAvailableWalkFuturesAndAddtoSelector(walkSelector));
        if (result.isEmpty()) {
            throw new IllegalStateException("No traversal walk futures detected, could this be a bug in the algorithm?");
        }

        // register all walk futures with a new selector.
        for (SelectableFuture<List<String>> futureWalk : result) {
            walkSelector.add(futureWalk);
        }
        // select walks as they become complete, although they should generally all be complete by this point.
        Set<String> remainingNodeIdsToThreadWalk = new LinkedHashSet<>(saga.nodes().stream().map(s -> s.id).collect(Collectors.toSet()));
        while (walkSelector.pending()) {
            SelectableFuture<List<String>> selected = walkSelector.select();
            long remainingMs = remaining(startTime, durationMs);
            List<String> walkedIds;
            try {
                walkedIds = selected.get(remainingMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw launder(e);
            }
            remainingNodeIdsToThreadWalk.removeAll(walkedIds);
            // potentially more walks available now that we have waited on the selected walk to complete
            result.addAll(drainAllAvailableWalkFuturesAndAddtoSelector(walkSelector));
        }

        // check that all nodes in saga were walked
        if (!remainingNodeIdsToThreadWalk.isEmpty()) {
            throw new IllegalStateException("Traversal failed. The following nodes were not walked: " + remainingNodeIdsToThreadWalk);
        }

        return result;
    }

    private ArrayList<SelectableFuture<List<String>>> drainAllAvailableWalkFuturesAndAddtoSelector(FutureSelector<List<String>> selector) {
        ArrayList<SelectableFuture<List<String>>> futureWalks = new ArrayList<>();
        pendingWalks.addAndGet(-futureThreadWalk.drainTo(futureWalks));
        for (SelectableFuture<List<String>> futureWalk : futureWalks) {
            selector.add(futureWalk);
        }
        return futureWalks;
    }

    private FutureSelector<VisitationResult> getVisitFutureSelector(long startTime, long durationMs) {
        FutureSelector<VisitationResult> visitSelector = new FutureSelector<>();
        for (SagaNode node : saga.nodes()) {
            Future<SelectableFuture<VisitationResult>> futureFuture = visitFutureById.computeIfAbsent(node.id, k -> new SimpleFuture<>());
            long remainingMs = remaining(startTime, durationMs);
            SelectableFuture<VisitationResult> selectableFuture;
            try {
                // should not block as all walks were complete
                selectableFuture = futureFuture.get(remainingMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw launder(e);
            }
            visitSelector.add(selectableFuture);
        }
        return visitSelector;
    }

    private long remaining(long startTime, long durationMs) {
        long remainingMs = durationMs - (System.currentTimeMillis() - startTime);
        if (remainingMs <= 0) {
            throw new TimeoutRuntimeException();
        }
        return remainingMs;
    }
}
