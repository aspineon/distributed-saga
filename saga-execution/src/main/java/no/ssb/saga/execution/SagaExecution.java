package no.ssb.saga.execution;

import no.ssb.concurrent.futureselector.SelectableFuture;
import no.ssb.concurrent.futureselector.SelectableThreadPoolExectutor;
import no.ssb.saga.api.Saga;
import no.ssb.saga.execution.adapter.AdapterLoader;
import no.ssb.saga.execution.adapter.SagaAdapter;
import no.ssb.saga.execution.adapter.VisitationResult;
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

    public SagaHandoffControl executeSaga(String requestData) {
        SelectableFuture<SagaHandoffResult> handoffFuture = new SelectableFuture<>(null);
        SelectableFuture<SagaHandoffResult> completionFuture = new SelectableFuture<>(null);
        UUID executionId = UUID.randomUUID();
        SagaTraversal sagaTraversal = new SagaTraversal(executorService, saga);
        SagaTraversalResult<VisitationResult<String>> traversalResult = sagaTraversal.forward(ste -> {
            SagaAdapter adapter = adapterLoader.load(ste.node);
            String inputJson = adapter.prepareJsonInputFromDependees(requestData, ste.previousResults);
            String startEntry = executionId + " " + ste.node.id + " " + adapter.name() + " INPUT: " + inputJson;
            sagaLog.write(ste.node, startEntry);
            if (Saga.ID_START.equals(ste.node.id)) {
                handoffFuture.complete(new SagaHandoffResult(executionId));
            }
            String outputJson = adapter.executeAction(inputJson);
            String endEntry = executionId + " " + ste.node.id + " " + adapter.name() + " OUTPUT: " + outputJson;
            sagaLog.write(ste.node, endEntry);
            if (Saga.ID_END.equals(ste.node.id)) {
                completionFuture.complete(new SagaHandoffResult(executionId));
            }
            return new VisitationResult<>(ste.node, outputJson);
        });
        SagaHandoffControl handoffControl = new SagaHandoffControl(traversalResult, handoffFuture, completionFuture);
        return handoffControl;
    }

    public SagaHandoffControl rollbackSaga(String requestData) {
        SelectableFuture<SagaHandoffResult> handoffFuture = new SelectableFuture<>(null);
        SelectableFuture<SagaHandoffResult> completionFuture = new SelectableFuture<>(null);
        UUID executionId = UUID.randomUUID();
        SagaTraversal sagaTraversal = new SagaTraversal(executorService, saga);
        SagaTraversalResult<VisitationResult<String>> traversalResult = sagaTraversal.backward(ste -> {
            SagaAdapter adapter = adapterLoader.load(ste.node);
            String inputJson = adapter.prepareJsonInputFromDependees(requestData, ste.previousResults);
            String startEntry = executionId + " " + ste.node.id + " " + adapter.name() + " COMP-INPUT: " + inputJson;
            sagaLog.write(ste.node, startEntry);
            if (Saga.ID_END.equals(ste.node.id)) {
                handoffFuture.complete(new SagaHandoffResult(executionId));
            }
            String outputJson = adapter.executeCompensatingAction(inputJson);
            String endEntry = executionId + " " + ste.node.id + " " + adapter.name() + " COMP-OUTPUT: " + outputJson;
            sagaLog.write(ste.node, endEntry);
            if (Saga.ID_START.equals(ste.node.id)) {
                completionFuture.complete(new SagaHandoffResult(executionId));
            }
            return new VisitationResult<>(ste.node, outputJson);
        });
        SagaHandoffControl handoffControl = new SagaHandoffControl(traversalResult, handoffFuture, completionFuture);
        return handoffControl;
    }
}
