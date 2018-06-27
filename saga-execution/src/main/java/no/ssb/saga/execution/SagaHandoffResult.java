package no.ssb.saga.execution;

import java.util.UUID;

public class SagaHandoffResult {
    public final UUID executionId;

    public SagaHandoffResult(UUID executionId) {
        this.executionId = executionId;
    }
}
