package no.ssb.saga.execution;

import no.ssb.concurrent.futureselector.SelectableFuture;

public class SagaHandoffControl {
    private final SagaTraversalResult traversalResult;
    private final SelectableFuture<SagaHandoffResult> handoffFuture;
    private final SelectableFuture<SagaHandoffResult> completionFuture;

    SagaHandoffControl(SagaTraversalResult traversalResult, SelectableFuture<SagaHandoffResult> handoffFuture, SelectableFuture<SagaHandoffResult> completionFuture) {
        this.traversalResult = traversalResult;
        this.handoffFuture = handoffFuture;
        this.completionFuture = completionFuture;
    }

    public SagaTraversalResult getTraversalResult() {
        return traversalResult;
    }

    public SelectableFuture<SagaHandoffResult> getHandoffFuture() {
        return handoffFuture;
    }

    public SelectableFuture<SagaHandoffResult> getCompletionFuture() {
        return completionFuture;
    }
}
