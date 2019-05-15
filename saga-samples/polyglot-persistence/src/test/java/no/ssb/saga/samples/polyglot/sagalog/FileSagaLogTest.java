package no.ssb.saga.samples.polyglot.sagalog;

import no.ssb.saga.execution.sagalog.SagaLog;
import no.ssb.saga.execution.sagalog.SagaLogEntry;
import no.ssb.saga.execution.sagalog.SagaLogEntryBuilder;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;

public class FileSagaLogTest {

    FileSagaLog createNewSagaLog() {
        Path path = Paths.get("target/test-sagalog.dat");
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new FileSagaLog(path);
    }

    @Test
    public void thatWriteAndReadEntriesWorks() {
        SagaLog<Long> sagaLog = createNewSagaLog();

        Deque<SagaLogEntry<Long>> expectedEntries = writeSuccessfulVanillaSagaExecutionEntries(sagaLog, UUID.randomUUID().toString());

        assertEquals(sagaLog.readEntries(expectedEntries.getFirst().getExecutionId()).collect(Collectors.toList()), expectedEntries);
    }

    @Test
    public void thatTruncateWithReadIncompleteWorks() {
        SagaLog<Long> sagaLog = createNewSagaLog();

        Deque<SagaLogEntry<Long>> initialEntries = writeSuccessfulVanillaSagaExecutionEntries(sagaLog, UUID.randomUUID().toString());
        sagaLog.truncate(initialEntries.getLast().getId());

        Deque<SagaLogEntry<Long>> expectedEntries = writeSuccessfulVanillaSagaExecutionEntries(sagaLog, UUID.randomUUID().toString());

        List<SagaLogEntry<Long>> actualEntries = sagaLog.readIncompleteSagas().collect(Collectors.toList());
        assertEquals(actualEntries, expectedEntries);
    }

    @Test
    public void thatNoTruncateWithReadIncompleteWorks() {
        SagaLog<Long> sagaLog = createNewSagaLog();

        Deque<SagaLogEntry<Long>> firstEntries = writeSuccessfulVanillaSagaExecutionEntries(sagaLog, UUID.randomUUID().toString());
        Deque<SagaLogEntry<Long>> secondEntries = writeSuccessfulVanillaSagaExecutionEntries(sagaLog, UUID.randomUUID().toString());
        Deque<SagaLogEntry<Long>> expectedEntries = new LinkedList<>();
        expectedEntries.addAll(firstEntries);
        expectedEntries.addAll(secondEntries);

        List<SagaLogEntry<Long>> actualEntries = sagaLog.readIncompleteSagas().collect(Collectors.toList());
        assertEquals(actualEntries, expectedEntries);
    }

    @Test
    public void thatSnapshotOfSagaLogEntriesByNodeIdWorks() {
        SagaLog<Long> sagaLog = createNewSagaLog();

        Deque<SagaLogEntry<Long>> firstEntries = writeSuccessfulVanillaSagaExecutionEntries(sagaLog, UUID.randomUUID().toString());
        Deque<SagaLogEntry<Long>> secondEntries = writeSuccessfulVanillaSagaExecutionEntries(sagaLog, UUID.randomUUID().toString());
        Deque<SagaLogEntry<Long>> expectedEntries = new LinkedList<>();
        expectedEntries.addAll(firstEntries);
        expectedEntries.addAll(secondEntries);

        Map<String, List<SagaLogEntry<Long>>> snapshotFirst = sagaLog.getSnapshotOfSagaLogEntriesByNodeId(firstEntries.getFirst().getExecutionId());
        Set<SagaLogEntry<Long>> firstFlattenedSnapshot = new LinkedHashSet<>();
        for (List<SagaLogEntry<Long>> collection : snapshotFirst.values()) {
            firstFlattenedSnapshot.addAll(collection);
        }
        Map<String, List<SagaLogEntry<Long>>> snapshotSecond = sagaLog.getSnapshotOfSagaLogEntriesByNodeId(secondEntries.getFirst().getExecutionId());
        Set<SagaLogEntry<Long>> secondFlattenedSnapshot = new LinkedHashSet<>();
        for (List<SagaLogEntry<Long>> collection : snapshotSecond.values()) {
            secondFlattenedSnapshot.addAll(collection);
        }

        assertEquals(firstFlattenedSnapshot, Set.copyOf(firstEntries));
        assertEquals(secondFlattenedSnapshot, Set.copyOf(secondEntries));
    }

    private Deque<SagaLogEntry<Long>> writeSuccessfulVanillaSagaExecutionEntries(SagaLog<Long> sagaLog, String executionId) {
        Deque<SagaLogEntryBuilder<Long>> entryBuilders = new LinkedList<>();
        entryBuilders.add(sagaLog.builder().startSaga(executionId, "Vanilla-Saga", "{}"));
        entryBuilders.add(sagaLog.builder().startAction(executionId, "action1"));
        entryBuilders.add(sagaLog.builder().startAction(executionId, "action2"));
        entryBuilders.add(sagaLog.builder().endAction(executionId, "action1", "{}"));
        entryBuilders.add(sagaLog.builder().endAction(executionId, "action2", "{}"));
        entryBuilders.add(sagaLog.builder().endSaga(executionId));

        Deque<SagaLogEntry<Long>> entries = new LinkedList<>();
        for (SagaLogEntryBuilder<Long> builder : entryBuilders) {
            CompletableFuture<SagaLogEntry<Long>> entryFuture = sagaLog.write(builder);
            entries.add(entryFuture.join());
        }
        return entries;
    }

    @Test
    public void thatSerializationAndDeserializationWorks() {
        FileSagaLog sagaLog = createNewSagaLog();
        checkSerializationAndDeserialization(sagaLog, sagaLog.builder().startSaga("ex-1234", "Some-test-saga", "{}"));
        checkSerializationAndDeserialization(sagaLog, sagaLog.builder().startSaga("ex-1234", "Saga Name With Spaces", "{}"));
        checkSerializationAndDeserialization(sagaLog, sagaLog.builder().endSaga("ex-1234"));
        checkSerializationAndDeserialization(sagaLog, sagaLog.builder().startAction("ex-1234", "abc-Start-Action"));
        checkSerializationAndDeserialization(sagaLog, sagaLog.builder().endAction("ex-1234", "abc-End-Action", "{}"));
        checkSerializationAndDeserialization(sagaLog, sagaLog.builder().abort("ex-1234", "abc-Abort"));
        checkSerializationAndDeserialization(sagaLog, sagaLog.builder().compDone("ex-1234", "abc-Comp-Done"));
    }

    private void checkSerializationAndDeserialization(FileSagaLog sagaLog, SagaLogEntryBuilder<Long> builder) {
        SagaLogEntry<Long> input = sagaLog.write(builder).join();
        byte[] serializedInput = sagaLog.serialize(input);
        SagaLogEntry output = sagaLog.deserialize(serializedInput);
        assertEquals(output, input);
        byte[] serializedOutput = sagaLog.serialize(output);
        assertEquals(serializedOutput, serializedInput);
    }

}
