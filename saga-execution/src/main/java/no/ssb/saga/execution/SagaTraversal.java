package no.ssb.saga.execution;

import no.ssb.concurrent.futureselector.FutureSelector;
import no.ssb.concurrent.futureselector.SelectableFuture;
import no.ssb.concurrent.futureselector.SelectableThreadPoolExectutor;
import no.ssb.concurrent.futureselector.SimpleFuture;
import no.ssb.concurrent.futureselector.Utils;
import no.ssb.saga.api.Saga;
import no.ssb.saga.api.SagaNode;
import no.ssb.saga.execution.adapter.VisitationResult;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    public SagaTraversalResult forward(Function<SagaTraversalElement, Object> visit) {
        return forward(null, null, visit);
    }

    public SagaTraversalResult backward(Function<SagaTraversalElement, Object> visit) {
        return backward(null, null, visit);
    }

    public SagaTraversalResult forward(
            SelectableFuture<SagaHandoffResult> handoffFuture,
            SelectableFuture<SagaHandoffResult> completionFuture,
            Function<SagaTraversalElement, Object> visit) {
        return traverse(true, saga.getStartNode(), handoffFuture, completionFuture, visit);
    }

    public SagaTraversalResult backward(
            SelectableFuture<SagaHandoffResult> handoffFuture,
            SelectableFuture<SagaHandoffResult> completionFuture,
            Function<SagaTraversalElement, Object> visit) {
        return traverse(false, saga.getEndNode(), handoffFuture, completionFuture, visit);
    }

    private SagaTraversalResult traverse(
            boolean forward,
            SagaNode firstNode,
            SelectableFuture<SagaHandoffResult> handoffFuture,
            SelectableFuture<SagaHandoffResult> completionFuture,
            Function<SagaTraversalElement, Object> visit) {

        AtomicInteger pendingWalks = new AtomicInteger(1);
        BlockingQueue<SelectableFuture<List<String>>> futureThreadWalk = new LinkedBlockingQueue<>();
        ConcurrentHashMap<String, SimpleFuture<SelectableFuture<VisitationResult>>> futureById = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, SagaNode> visitedById = new ConcurrentHashMap<>();
        visitedById.putIfAbsent(firstNode.id, firstNode);
        SelectableFuture<List<String>> future = (SelectableFuture<List<String>>) executorService.submit(() -> {
                    try {
                        return traverse(
                                pendingWalks,
                                futureThreadWalk,
                                new ArrayList<>(),
                                forward,
                                firstNode,
                                new LinkedList<>(),
                                visitedById,
                                futureById,
                                handoffFuture,
                                completionFuture,
                                visit
                        );
                    } catch (Throwable t) {
                        if (handoffFuture != null) {
                            handoffFuture.executionException(t);
                        }
                        if (completionFuture != null) {
                            completionFuture.executionException(t);
                        }
                        throw Utils.launder(t);
                    }
                }
        );
        futureThreadWalk.add(future);
        return new SagaTraversalResult(saga, pendingWalks, futureThreadWalk, futureById);
    }

    private List<String> traverse(
            AtomicInteger pendingWalks,
            BlockingQueue<SelectableFuture<List<String>>> futureThreadWalk,
            List<String> traversedInThread,
            boolean forward,
            SagaNode node,
            Deque<SagaNode> ancestors,
            ConcurrentMap<String, SagaNode> visitedById,
            ConcurrentMap<String, SimpleFuture<SelectableFuture<VisitationResult>>> futureById,
            SelectableFuture<SagaHandoffResult> handoffFuture,
            SelectableFuture<SagaHandoffResult> completionFuture,
            Function<SagaTraversalElement, Object> visit) {

        traversedInThread.add(node.id);

        /*
         * Wait for visitation of all nodes this node depends on to complete
         */
        Map<SagaNode, Object> outputByNode = new LinkedHashMap<>();
        if ((forward ? node.incoming.size() : node.outgoing.size()) > 0) {
            // Add to selector all visitation-futures this node depends on
            FutureSelector<VisitationResult> selector = new FutureSelector<>();
            for (SagaNode dependOnNode : (forward ? node.incoming : node.outgoing)) {
                SimpleFuture<SelectableFuture<VisitationResult>> dependOnSimpleFuture = futureById.computeIfAbsent(dependOnNode.id, k -> new SimpleFuture<>());
                SelectableFuture<VisitationResult> selectableFuture;
                try {
                    selectableFuture = dependOnSimpleFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw Utils.launder(e);
                }
                selector.add(selectableFuture);
            }
            // Use selector to collect all visitation results
            while (selector.pending()) {
                SelectableFuture<VisitationResult> selected = selector.select(); // block until a result is available
                VisitationResult v;
                try {
                    v = selected.get(); // will never block
                } catch (InterruptedException | ExecutionException e) {
                    throw Utils.launder(e);
                }
                outputByNode.put(v.node, v.output);
            }
        }

        /*
         * Visit this node within the walking thread
         */
        SimpleFuture<SelectableFuture<VisitationResult>> futureResult = futureById.computeIfAbsent(node.id, k -> new SimpleFuture<>());
        try {
            Object result = visit.apply(new SagaTraversalElement(outputByNode, ancestors, node));
            SelectableFuture<VisitationResult> selectableFuture = new SelectableFuture<>(() -> new VisitationResult(node, result));
            selectableFuture.run();
            futureResult.complete(selectableFuture);
        } catch (Throwable t) {
            futureResult.executionException(t);
            throw Utils.launder(t);
        }

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
            SelectableFuture<List<String>> future = (SelectableFuture<List<String>>) executorService.submit(() -> {
                try {
                    return traverse(pendingWalks, futureThreadWalk, new ArrayList<>(), forward, child, childAncestors, visitedById, futureById, handoffFuture, completionFuture, visit);
                } catch (Throwable t) {
                    if (handoffFuture != null) {
                        handoffFuture.executionException(t);
                    }
                    if (completionFuture != null) {
                        completionFuture.executionException(t);
                    }
                    throw Utils.launder(t);
                }
            });
            futureThreadWalk.add(future);
            pendingWalks.incrementAndGet();
        }

        // traverse last child within this thread
        return traverse(pendingWalks, futureThreadWalk, traversedInThread, forward, effectiveChildren.get(effectiveChildren.size() - 1), childAncestors, visitedById, futureById, handoffFuture, completionFuture, visit);
    }
}
