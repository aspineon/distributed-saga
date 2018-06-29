package no.ssb.saga.execution.adapter;

import no.ssb.saga.api.SagaNode;

public class VisitationResult {
    public final SagaNode node;
    public final Object output;

    public VisitationResult(SagaNode node, Object output) {
        this.node = node;
        this.output = output;
    }
}
