package no.ssb.saga.execution.sagalog;

import no.ssb.saga.api.SagaNode;
import no.ssb.saga.execution.adapter.ActionOutputSerializer;

public interface SagaLog {

    boolean ACTION = true;
    boolean COMPENSATING_ACTION = false;

    boolean BEFORE = true;
    boolean AFTER = false;

    String write(boolean beforeOrAfter, boolean actionOrCompensatingAction, SagaNode node, String executionId, ActionOutputSerializer serializer, Object data);
}
