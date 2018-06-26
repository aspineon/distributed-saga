package no.ssb.saga.execution;

import no.ssb.concurrent.futureselector.SelectableThreadPoolExectutor;
import no.ssb.saga.api.Saga;
import no.ssb.saga.execution.adapter.AdapterLoader;
import no.ssb.saga.execution.adapter.SagaAdapter;
import no.ssb.saga.execution.adapter.VisitationResult;
import no.ssb.saga.execution.sagalog.SagaLog;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    public void executeSaga(String requestData, long timeout, TimeUnit unit) {
        UUID executionId = UUID.randomUUID();
        SagaTraversal sagaTraversal = new SagaTraversal(executorService, saga);
        SagaTraversalResult<VisitationResult<String>> traversalResult = sagaTraversal.forward(ste -> {
            SagaAdapter adapter = adapterLoader.load(ste.node);
            String inputJson = adapter.prepareJsonInputFromDependees(requestData, ste.previousResults);
            String startEntry = executionId + " " + ste.node.id + " " + adapter.name() + " INPUT: " + inputJson;
            sagaLog.write(ste.node, startEntry);
            String outputJson = adapter.executeAction(inputJson);
            String endEntry = executionId + " " + ste.node.id + " " + adapter.name() + " OUTPUT: " + outputJson;
            sagaLog.write(ste.node, endEntry);
            return new VisitationResult<>(ste.node, outputJson);
        });
        traversalResult.waitForCompletion(timeout, unit);
    }

    public void rollbackSaga(String requestData, long timeout, TimeUnit unit) {
        UUID executionId = UUID.randomUUID();
        SagaTraversal sagaTraversal = new SagaTraversal(executorService, saga);
        SagaTraversalResult<VisitationResult<String>> traversalResult = sagaTraversal.backward(ste -> {
            SagaAdapter adapter = adapterLoader.load(ste.node);
            String inputJson = adapter.prepareJsonInputFromDependees(requestData, ste.previousResults);
            String startEntry = executionId + " " + ste.node.id + " " + adapter.name() + " COMP-INPUT: " + inputJson;
            sagaLog.write(ste.node, startEntry);
            String outputJson = adapter.executeCompensatingAction(inputJson);
            String endEntry = executionId + " " + ste.node.id + " " + adapter.name() + " COMP-OUTPUT: " + outputJson;
            sagaLog.write(ste.node, endEntry);
            return new VisitationResult<>(ste.node, outputJson);
        });
        traversalResult.waitForCompletion(timeout, unit);
    }
}
