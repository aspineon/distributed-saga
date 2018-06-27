package no.ssb.saga.execution;

import no.ssb.concurrent.futureselector.SelectableThreadPoolExectutor;
import no.ssb.saga.api.Saga;
import no.ssb.saga.execution.adapter.AdapterLoader;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;

public class SagaExecutionTest {

    private static final AtomicLong nextWorkerId = new AtomicLong(1);

    private static final SelectableThreadPoolExectutor executorService = new SelectableThreadPoolExectutor(
            5, 20,
            60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("execution-test-worker-" + nextWorkerId.getAndIncrement());
                thread.setUncaughtExceptionHandler((t, e) -> {
                    System.err.println("Uncaught exception in thread " + thread.getName());
                    e.printStackTrace();
                });
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy()
    );

    private static final AdapterLoader adapterLoader = new AdapterLoader()
            .register(new SagaAdapterGeneric());

    private void executeAndVerifyThatActionsWereExecuted(String requestData, Saga saga) {
        Set<String> nodesLogged = Collections.synchronizedSet(new LinkedHashSet<>());
        SagaExecution sagaExecution = new SagaExecution((node, data) -> {
            nodesLogged.add(node.id);
            System.out.println(data);
            return "{}";
        }, executorService, saga, adapterLoader);
        sagaExecution.executeSaga(requestData).getTraversalResult().waitForCompletion(1, TimeUnit.MINUTES);
        assertEquals(nodesLogged, saga.nodes().stream().map(n -> n.id).collect(Collectors.toSet()));
    }

    private void rollbackAndVerifyThatCompensatingActionsWereExecuted(String requestData, Saga saga) {
        Set<String> nodesLogged = Collections.synchronizedSet(new LinkedHashSet<>());
        SagaExecution sagaExecution = new SagaExecution((node, data) -> {
            nodesLogged.add(node.id);
            System.out.println(data);
            return "{}";
        }, executorService, saga, adapterLoader);
        sagaExecution.rollbackSaga(requestData).getTraversalResult().waitForCompletion(1, TimeUnit.MINUTES);
        assertEquals(nodesLogged, saga.nodes().stream().map(n -> n.id).collect(Collectors.toSet()));
    }

    @Test
    public void thatEmptySagaExecutes() {
        executeAndVerifyThatActionsWereExecuted("{\"request-data\":\"empty-saga-data\"}", Saga
                .start("The Empty Saga").linkToEnd()
                .end()
        );
    }

    @Test
    public void thatEmptySagaRollsBack() {
        rollbackAndVerifyThatCompensatingActionsWereExecuted("{\"request-data\":\"empty-saga-data\"}", Saga
                .start("The Empty Saga").linkToEnd()
                .end()
        );
    }

    @Test
    public void thatAllActionsFromComplexSagaIsExecuted() {
        executeAndVerifyThatActionsWereExecuted("{\"request-data\":\"complex-saga-data\"}", Saga
                .start("The complex saga").linkTo("A1", "A2")
                .id("A1").adapter(SagaAdapterGeneric.NAME).linkToEnd()
                .id("A2").adapter(SagaAdapterGeneric.NAME).linkTo("B1", "B2")
                .id("B1").adapter(SagaAdapterGeneric.NAME).linkToEnd()
                .id("B2").adapter(SagaAdapterGeneric.NAME).linkTo("C1", "C2", "C3")
                .id("C1").adapter(SagaAdapterGeneric.NAME).linkToEnd()
                .id("C2").adapter(SagaAdapterGeneric.NAME).linkToEnd()
                .id("C3").adapter(SagaAdapterGeneric.NAME).linkTo("B1")
                .end()
        );
    }

    @Test
    public void thatAllCompensatingActionsFromComplexSagaAreExecutedOnRollback() {
        rollbackAndVerifyThatCompensatingActionsWereExecuted("{\"request-data\":\"complex-saga-data\"}", Saga
                .start("The complex saga").linkTo("A1", "A2")
                .id("A1").adapter(SagaAdapterGeneric.NAME).linkToEnd()
                .id("A2").adapter(SagaAdapterGeneric.NAME).linkTo("B1", "B2")
                .id("B1").adapter(SagaAdapterGeneric.NAME).linkToEnd()
                .id("B2").adapter(SagaAdapterGeneric.NAME).linkTo("C1", "C2", "C3")
                .id("C1").adapter(SagaAdapterGeneric.NAME).linkToEnd()
                .id("C2").adapter(SagaAdapterGeneric.NAME).linkToEnd()
                .id("C3").adapter(SagaAdapterGeneric.NAME).linkTo("B1")
                .end()
        );
    }
}
