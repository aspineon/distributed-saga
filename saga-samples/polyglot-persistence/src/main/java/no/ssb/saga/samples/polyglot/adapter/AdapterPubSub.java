package no.ssb.saga.samples.polyglot.adapter;

import no.ssb.saga.execution.adapter.SagaAdapter;
import no.ssb.saga.execution.adapter.VisitationResult;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class AdapterPubSub implements SagaAdapter {

    public static final String NAME = "PubSub";

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
        output.put("action", "Publish that data has been created to topic");
        output.put("topic", "/mother/of/all/topics");
        JSONObject event = new JSONObject();
        event.put("id", String.valueOf(UUID.randomUUID()));
        event.put("category", "polyglot sample");
        output.put("event", event);
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
        output.put("compensating action", "Publish that data has been removed to topic");
        output.put("topic", "/mother/of/all/topics");
        JSONObject event = new JSONObject();
        event.put("id", String.valueOf(UUID.randomUUID()));
        event.put("category", "polyglot sample");
        output.put("event", event);
        output.put("txid", UUID.randomUUID());
        root.put("output", output);
        return root.toString();
    }
}
