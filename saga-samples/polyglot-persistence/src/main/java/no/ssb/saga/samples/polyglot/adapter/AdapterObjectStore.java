package no.ssb.saga.samples.polyglot.adapter;

import no.ssb.saga.execution.adapter.SagaAdapter;
import no.ssb.saga.execution.adapter.VisitationResult;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class AdapterObjectStore implements SagaAdapter {

    public static final String NAME = "ObjectStore";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String prepareJsonInputFromDependees(String originalRequestJson, List<VisitationResult<String>> dependeesOutput) {
        return originalRequestJson;
    }

    @Override
    public String executeAction(String inputJson) {
        JSONObject inputRoot = new JSONObject(inputJson);
        JSONObject root = new JSONObject();
        root.put("input", inputRoot);
        JSONObject output = new JSONObject();
        output.put("action", "Upload data as object to bucket X");
        JSONObject http = new JSONObject();
        http.put("method", "PUT");
        http.put("Content-Type", "text/plain; charset=utf-8");
        http.put("url", "http://some.objectstore.com/bucket/X/path/to/object/with/id/" + UUID.randomUUID());
        http.put("response-code", "201");
        output.put("http", http);
        output.put("txid", UUID.randomUUID());
        root.put("output", output);
        return root.toString();
    }

    @Override
    public String executeCompensatingAction(String inputJson) {
        JSONObject inputRoot = new JSONObject(inputJson);
        JSONObject root = new JSONObject();
        root.put("input", inputRoot);
        JSONObject output = new JSONObject();
        output.put("compensating action", "Delete object from bucket X");
        JSONObject http = new JSONObject();
        http.put("method", "DELETE");
        http.put("url", "http://some.objectstore.com/bucket/X/path/to/object/with/id/" + UUID.randomUUID());
        http.put("response-code", "200");
        output.put("http", http);
        output.put("txid", UUID.randomUUID());
        root.put("output", output);
        return root.toString();
    }
}
