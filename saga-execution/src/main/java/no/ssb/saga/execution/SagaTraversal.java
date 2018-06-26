package no.ssb.saga.execution;

import no.ssb.concurrent.futureselector.FutureSelector;
import no.ssb.concurrent.futureselector.SelectableFuture;
import no.ssb.concurrent.futureselector.SelectableThreadPoolExectutor;
import no.ssb.concurrent.futureselector.SimpleFuture;
import no.ssb.saga.api.Saga;
import no.ssb.saga.api.SagaNode;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class SagaTraversal {

    private final SelectableThreadPoolExectutor executorService;
    private final Saga saga;

    public SagaTraversal(SelectableThreadPoolExectutor executorService, Saga saga) {
        this.executorService = executorService;
        this.saga = saga;
    }

    public <V> SagaTraversalResult<V> forward(Function<SagaTraversalElement, V> visit) {
        return traverse(true, saga.getStartNode(), saga.getEndNode(), visit);
    }

    public <V> SagaTraversalResult<V> backward(Function<SagaTraversalElement, V> visit) {
        return traverse(false, saga.getEndNode(), saga.getStartNode(), visit);
    }

    private <V> SagaTraversalResult<V> traverse(boolean forward, SagaNode firstNode, SagaNode lastNode, Function<SagaTraversalElement, V> visit) {
        AtomicInteger pendingWalks = new AtomicInteger(1);
        BlockingQueue<SelectableFuture<List<String>>> futureThreadWalk = new LinkedBlockingQueue<>();
        ConcurrentHashMap<String, SimpleFuture<SelectableFuture<V>>> futureById = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, SagaNode> visitedById = new ConcurrentHashMap<>();
        visitedById.putIfAbsent(firstNode.id, firstNode);
        SelectableFuture<List<String>> future = (SelectableFuture<List<String>>) executorService.submit(() ->
                traverse(
                        pendingWalks,
                        futureThreadWalk,
                        new ArrayList<>(),
                        forward,
                        firstNode,
                        new LinkedList<>(),
                        visitedById,
                        futureById,
                        visit
                )
        );
        futureThreadWalk.add(future);
        return new SagaTraversalResult<>(saga, pendingWalks, futureThreadWalk, futureById);
    }

    private <V> List<String> traverse(
            AtomicInteger pendingWalks,
            BlockingQueue<SelectableFuture<List<String>>> futureThreadWalk,
            List<String> traversedInThread,
            boolean forward,
            SagaNode node,
            Deque<SagaNode> ancestors,
            ConcurrentMap<String, SagaNode> visitedById,
            ConcurrentMap<String, SimpleFuture<SelectableFuture<V>>> futureById,
            Function<SagaTraversalElement, V> visit) throws InterruptedException, ExecutionException {

        traversedInThread.add(node.id);

        /*
         * Wait for visitation of all nodes this node depends on to complete
         */
        List<V> results = new ArrayList<>();
        if ((forward ? node.incoming.size() : node.outgoing.size()) > 0) {
            // Add to selector all visitation-futures this node depends on
            FutureSelector<V> selector = new FutureSelector<>();
            for (SagaNode dependOnNode : (forward ? node.incoming : node.outgoing)) {
                SimpleFuture<SelectableFuture<V>> dependOnSimpleFuture = futureById.computeIfAbsent(dependOnNode.id, k -> new SimpleFuture<>());
                SelectableFuture<V> selectableFuture = dependOnSimpleFuture.get();
                selector.add(selectableFuture);
            }
            // Use selector to collect all visitation results
            while (selector.pending()) {
                SelectableFuture<V> selected = selector.select(); // block until a result is available
                V v = selected.get(); // will never block
                results.add(v);
            }
        }

        /*
         * Visit this node within the walking thread
         */
        SimpleFuture<SelectableFuture<V>> futureResult = futureById.computeIfAbsent(node.id, k -> new SimpleFuture<>());
        V result = visit.apply(new SagaTraversalElement(results, ancestors, node));
        SelectableFuture<V> selectableFuture = new SelectableFuture<V>(() -> result);
        selectableFuture.run();
        futureResult.complete(selectableFuture);

        /*
         * Traverse children
         */
        Deque<SagaNode> childAncestors = new LinkedList<>(ancestors);
        childAncestors.addLast(node);
        List<SagaNode> effectiveChildren = new ArrayList<>();
        for (SagaNode child : (forward ? node.outgoing : node.incoming)) {
            if (visitedById.putIfAbsent(child.id, child) != null) {
                continue; // someone else is already traversing this child in parallel
            }
            // first traversal of child
            effectiveChildren.add(child);
        }
        if (effectiveChildren.isEmpty()) {
            return traversedInThread; // no children, or children already being traversed in parallel
        }
        // traverse all but last child asynchronously
        for (int i = 0; i < effectiveChildren.size() - 1; i++) {
            SagaNode child = effectiveChildren.get(i);
            SelectableFuture<List<String>> future = (SelectableFuture<List<String>>) executorService.submit(() -> traverse(pendingWalks, futureThreadWalk, new ArrayList<>(), forward, child, childAncestors, visitedById, futureById, visit));
            futureThreadWalk.add(future);
            pendingWalks.incrementAndGet();
        }

        // traverse last child within this thread
        return traverse(pendingWalks, futureThreadWalk, traversedInThread, forward, effectiveChildren.get(effectiveChildren.size() - 1), childAncestors, visitedById, futureById, visit);
    }
}
