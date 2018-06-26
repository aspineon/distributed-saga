package no.ssb.saga.execution.sagalog;

import no.ssb.saga.api.SagaNode;

public interface SagaLog {

    String write(SagaNode node, String data);
}
