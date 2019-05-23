package no.ssb.saga.samples.polyglot.sagalog;

import no.ssb.sagalog.SagaLogInitializer;
import no.ssb.sagalog.SagaLogPool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class FileSagaLogInitializer implements SagaLogInitializer {

    public FileSagaLogInitializer() {
    }

    public SagaLogPool initialize(Map<String, String> configuration) {
        Path folder = Paths.get(configuration.get("filesagalog.folder"));
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new FileSagaLogPool(folder);
    }

    public Map<String, String> configurationOptionsAndDefaults() {
        return Map.of("filesagalog.folder", "target/test-sagalog");
    }
}
