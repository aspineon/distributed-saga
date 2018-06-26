package no.ssb.saga.execution.adapter;

import no.ssb.saga.api.SagaNode;

public class VisitationResult<V> {
    public final SagaNode node;
    public final V result;

    public VisitationResult(SagaNode node, V result) {
        this.node = node;
        this.result = result;
    }
}
