package no.ssb.saga.samples.polyglot.sagalog;

import no.ssb.saga.api.SagaNode;
import no.ssb.saga.execution.adapter.ActionOutputSerializer;
import no.ssb.saga.execution.sagalog.SagaLog;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class InMemorySagaLog implements SagaLog {

    public final Deque<String> entries = new ConcurrentLinkedDeque<>();

    @Override
    public String write(boolean beforeOrAfter, boolean actionOrCompensatingAction, SagaNode node, String executionId, ActionOutputSerializer serializer, Object data) {
        String serializedEntry = serializer.serialize(data);
        entries.addLast(serializedEntry);
        return "{\"logid\":\"" + System.currentTimeMillis() + "\"}";
    }
}
