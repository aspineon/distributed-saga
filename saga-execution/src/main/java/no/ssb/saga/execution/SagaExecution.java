package no.ssb.saga.execution;

import no.ssb.concurrent.futureselector.SelectableFuture;
import no.ssb.concurrent.futureselector.SelectableThreadPoolExectutor;
import no.ssb.saga.api.Saga;
import no.ssb.saga.execution.adapter.AdapterLoader;
import no.ssb.saga.execution.adapter.SagaAdapter;
import no.ssb.saga.execution.sagalog.SagaLog;

import java.util.UUID;

public class SagaExecution {

    private final SagaLog sagaLog;
    private final SelectableThreadPoolExectutor executorService;
    private final Saga saga;
    private final AdapterLoader adapterLoader;

    public SagaExecution(SagaLog sagaLog, SelectableThreadPoolExectutor executorService, Saga saga, AdapterLoader adapterLoader) {
        this.sagaLog = sagaLog;
        this.executorService = executorService;
        this.saga = saga;
        this.adapterLoader = adapterLoader;
    }

    /**
     * @param requestData the data to pass as input to start-node.
     * @return
     */
    public SagaHandoffControl executeSaga(Object requestData) {
        SelectableFuture<SagaHandoffResult> handoffFuture = new SelectableFuture<>(null);
        SelectableFuture<SagaHandoffResult> completionFuture = new SelectableFuture<>(null);
        UUID executionId = UUID.randomUUID();
        SagaTraversal sagaTraversal = new SagaTraversal(executorService, saga);
        SagaTraversalResult traversalResult = sagaTraversal.forward(handoffFuture, completionFuture, ste -> {
            SagaAdapter adapter = adapterLoader.load(ste.node);
            if (Saga.ID_END.equals(ste.node.id)) {
                sagaLog.write(SagaLog.AFTER, SagaLog.ACTION, ste.node, executionId.toString(), adapter.serializer(), null);
                completionFuture.complete(new SagaHandoffResult(executionId));
                return null;
            }
            if (Saga.ID_START.equals(ste.node.id)) {
                sagaLog.write(SagaLog.BEFORE, SagaLog.ACTION, ste.node, executionId.toString(), adapter.serializer(), requestData);
                handoffFuture.complete(new SagaHandoffResult(executionId));
                return null;
            }
            Object input = adapter.prepareInputFromDependees(requestData, ste.outputByNode);
            sagaLog.write(SagaLog.BEFORE, SagaLog.ACTION, ste.node, executionId.toString(), adapter.serializer(), input);
            Object output = adapter.executeAction(input); // safe unchecked call
            sagaLog.write(SagaLog.AFTER, SagaLog.ACTION, ste.node, executionId.toString(), adapter.serializer(), output);
            return output;
        });
        return new SagaHandoffControl(traversalResult, handoffFuture, completionFuture);
    }

    public SagaHandoffControl rollbackSaga(Object requestData) {
        SelectableFuture<SagaHandoffResult> handoffFuture = new SelectableFuture<>(null);
        SelectableFuture<SagaHandoffResult> completionFuture = new SelectableFuture<>(null);
        UUID executionId = UUID.randomUUID();
        SagaTraversal sagaTraversal = new SagaTraversal(executorService, saga);
        SagaTraversalResult traversalResult = sagaTraversal.forward(handoffFuture, completionFuture, ste -> {
            SagaAdapter adapter = adapterLoader.load(ste.node);
            if (Saga.ID_END.equals(ste.node.id)) {
                sagaLog.write(SagaLog.BEFORE, SagaLog.COMPENSATING_ACTION, ste.node, executionId.toString(), adapter.serializer(), null);
                completionFuture.complete(new SagaHandoffResult(executionId));
                return null;
            }
            if (Saga.ID_START.equals(ste.node.id)) {
                sagaLog.write(SagaLog.AFTER, SagaLog.COMPENSATING_ACTION, ste.node, executionId.toString(), adapter.serializer(), null);
                handoffFuture.complete(new SagaHandoffResult(executionId));
                return null;
            }
            Object input = adapter.prepareInputFromDependees(requestData, ste.outputByNode);
            sagaLog.write(SagaLog.BEFORE, SagaLog.COMPENSATING_ACTION, ste.node, executionId.toString(), adapter.serializer(), input);
            Object output = adapter.executeCompensatingAction(input); // safe unchecked call
            sagaLog.write(SagaLog.AFTER, SagaLog.COMPENSATING_ACTION, ste.node, executionId.toString(), adapter.serializer(), output);
            return output;
        });
        return new SagaHandoffControl(traversalResult, handoffFuture, completionFuture);
    }
}
