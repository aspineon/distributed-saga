package no.ssb.saga.samples.polyglot.sagalog;

import no.ssb.saga.execution.sagalog.SagaLog;
import no.ssb.saga.execution.sagalog.SagaLogEntry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileSagaLog implements SagaLog {

    public final File sagaLogFile;

    public FileSagaLog(File sagaLogFile) {
        this.sagaLogFile = sagaLogFile;
        try {
            // truncate file
            new FileOutputStream(sagaLogFile, false).close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String write(SagaLogEntry entry) {
        synchronized (sagaLogFile) {
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sagaLogFile, true), StandardCharsets.UTF_8))) {
                bw.write(entry.toString());
                bw.newLine();
                bw.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return "{\"logid\":\"" + System.currentTimeMillis() + "\"}";
    }

    @Override
    public List<SagaLogEntry> readEntries(String executionId) {
        List<SagaLogEntry> result = new ArrayList<>();
        synchronized (sagaLogFile) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(sagaLogFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    result.add(SagaLogEntry.from(line));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
}
