package no.ssb.saga.execution;

import no.ssb.saga.api.SagaNode;

import java.util.Deque;
import java.util.List;

public class SagaTraversalElement<V> {
    public final List<V> previousResults;
    public final Deque<SagaNode> ancestors;
    public final SagaNode node;

    public SagaTraversalElement(List<V> previousResults, Deque<SagaNode> ancestors, SagaNode node) {
        this.previousResults = previousResults;
        this.ancestors = ancestors;
        this.node = node;
    }
}
