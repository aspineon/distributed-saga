package no.ssb.saga.samples.polyglot.sagalog;

import com.squareup.tape2.QueueFile;
import no.ssb.saga.api.Saga;
import no.ssb.saga.execution.sagalog.SagaLog;
import no.ssb.saga.execution.sagalog.SagaLogEntry;
import no.ssb.saga.execution.sagalog.SagaLogEntryBuilder;
import no.ssb.saga.execution.sagalog.SagaLogEntryType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class FileSagaLog implements SagaLog<Long> {

    private AtomicLong nextId = new AtomicLong(0);
    private final QueueFile queueFile;

    public FileSagaLog(Path path) {
        try {
            queueFile = new QueueFile.Builder(path.toFile()).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<SagaLogEntry<Long>> write(SagaLogEntryBuilder<Long> builder) {
        synchronized (queueFile) {
            if (builder.id() == null) {
                builder.id(nextId.getAndIncrement());
            }
            SagaLogEntry<Long> entry = builder.build();
            try {
                queueFile.add(serialize(entry));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return CompletableFuture.completedFuture(entry);
        }
    }

    @Override
    public CompletableFuture<Void> truncate(Long id) {
        synchronized (queueFile) {
            Iterator<byte[]> iterator = queueFile.iterator();
            int n = 0;
            while (iterator.hasNext()) {
                SagaLogEntry entry = deserialize(iterator.next());
                n++;
                if (id.equals(entry.getId())) {
                    try {
                        queueFile.remove(n);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return CompletableFuture.completedFuture(null);
                }
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Stream<SagaLogEntry<Long>> readIncompleteSagas() {
        List<byte[]> list = new ArrayList<>();
        synchronized (queueFile) {
            for (byte[] bytes : queueFile) {
                list.add(bytes);
            }
        }
        return list.stream().map(this::deserialize);
    }

    @Override
    public Stream<SagaLogEntry<Long>> readEntries(String executionId) {
        return readIncompleteSagas().filter(entry -> executionId.equals(entry.getExecutionId()));
    }

    byte[] serialize(SagaLogEntry<Long> entry) {
        String serializedString = entry.getId()
                + " " + entry.getExecutionId()
                + " " + entry.getEntryType()
                + " " + entry.getNodeId()
                + (entry.getSagaName() == null ? "" : " " + entry.getSagaName())
                + (entry.getJsonData() == null ? "" : " " + entry.getJsonData());
        return serializedString.getBytes(StandardCharsets.UTF_8);
    }

    SagaLogEntry<Long> deserialize(byte[] bytes) {
        String serialized = new String(bytes, StandardCharsets.UTF_8);
        SagaLogEntryBuilder<Long> builder = builder();

        // mandatory log-fields

        int idEndIndex = serialized.indexOf(' ');
        Long id = Long.parseLong(serialized.substring(0, idEndIndex));
        serialized = serialized.substring(idEndIndex + 1);

        builder.id(id);

        int executionIdEndIndex = serialized.indexOf(' ');
        String executionId = serialized.substring(0, executionIdEndIndex);
        serialized = serialized.substring(executionIdEndIndex + 1);

        builder.executionId(executionId);

        int entryTypeEndIndex = serialized.indexOf(' ');
        SagaLogEntryType entryType = SagaLogEntryType.valueOf(serialized.substring(0, entryTypeEndIndex));
        serialized = serialized.substring(entryTypeEndIndex + 1);

        builder.entryType(entryType);

        int nodeIdEndIdex = serialized.indexOf(' ');
        if (nodeIdEndIdex == -1) {
            return builder.nodeId(serialized).build();
        }

        String nodeId = serialized.substring(0, nodeIdEndIdex);
        serialized = serialized.substring(nodeIdEndIdex + 1);

        builder.nodeId(nodeId);

        // optional log-fields
        if (Saga.ID_START.equals(nodeId)) {
            int jsonDataBeginIndex = serialized.indexOf('{');
            if (jsonDataBeginIndex == -1) {
                String sagaName = serialized.substring(0, serialized.length() - 1);
                return builder.sagaName(sagaName).build();
            }
            String sagaName = serialized.substring(0, jsonDataBeginIndex - 1);
            String jsonData = serialized.substring(jsonDataBeginIndex);
            return builder.sagaName(sagaName).jsonData(jsonData).build();
        }

        int jsonDataBeginIndex = serialized.indexOf('{');
        if (jsonDataBeginIndex == -1) {
            return builder.build();
        }
        String jsonData = serialized.substring(jsonDataBeginIndex);
        return builder.jsonData(jsonData).build();
    }
}
