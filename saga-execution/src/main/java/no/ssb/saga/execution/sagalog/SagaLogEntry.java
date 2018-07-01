package no.ssb.saga.execution.sagalog;

import no.ssb.saga.api.Saga;

public class SagaLogEntry {

    public static SagaLogEntry startSaga(String executionId, String sagaName, String sagaInputJson) {
        return new SagaLogEntry(executionId, SagaLogEntryType.Start, Saga.ID_START, sagaName, sagaInputJson);
    }

    public static SagaLogEntry startAction(String executionId, String nodeId) {
        return new SagaLogEntry(executionId, SagaLogEntryType.Start, nodeId, null, null);
    }

    public static SagaLogEntry endAction(String executionId, String nodeId, String actionOutputJson) {
        return new SagaLogEntry(executionId, SagaLogEntryType.End, nodeId, null, actionOutputJson);
    }

    public static SagaLogEntry endSaga(String executionId) {
        return new SagaLogEntry(executionId, SagaLogEntryType.End, Saga.ID_END, null, null);
    }

    public static SagaLogEntry abort(String executionId, String nodeId) {
        return new SagaLogEntry(executionId, SagaLogEntryType.Abort, nodeId, null, null);
    }

    public static SagaLogEntry compDone(String executionId, String nodeId) {
        return new SagaLogEntry(executionId, SagaLogEntryType.Comp, nodeId, null, null);
    }

    public final String executionId;
    public final SagaLogEntryType entryType;
    public final String nodeId;
    public final String sagaName;
    public final String jsonData;

    public SagaLogEntry(String executionId, SagaLogEntryType entryType, String nodeId, String sagaName, String jsonData) {
        this.executionId = executionId;
        this.entryType = entryType;
        this.nodeId = nodeId;
        this.sagaName = sagaName;
        this.jsonData = jsonData;
    }

    @Override
    public String toString() {
        return executionId + ' ' + entryType + ' ' + nodeId + (sagaName == null ? "" : ' ' + sagaName) + (jsonData == null ? "" : ' ' + jsonData);
    }
}
