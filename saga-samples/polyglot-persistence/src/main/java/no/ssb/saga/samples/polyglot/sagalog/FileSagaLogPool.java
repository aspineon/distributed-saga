package no.ssb.saga.samples.polyglot.sagalog;


import no.ssb.sagalog.SagaLog;
import no.ssb.sagalog.SagaLogPool;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

class FileSagaLogPool implements SagaLogPool {

    private final Map<String, FileSagaLog> sagaLogByLogId = new ConcurrentHashMap<>();
    private final Set<String> occupied = new ConcurrentSkipListSet<>();
    private final Path folder;

    FileSagaLogPool(Path folder) {
        this.folder = folder;
    }

    @Override
    public SagaLog connect(String logId) {
        if (!occupied.add(logId)) {
            throw new RuntimeException(String.format("saga-log with logId \"%s\" already connected."));
        }
        FileSagaLog sagaLog = sagaLogByLogId.computeIfAbsent(logId, lid -> new FileSagaLog(Paths.get(folder.toString(), logId + ".dat")));
        return sagaLog;
    }

    @Override
    public void release(String logId) {
        occupied.remove(logId);
    }

    @Override
    public void shutdown() {
        sagaLogByLogId.clear();
        occupied.clear();
    }
}
