package no.ssb.saga.samples.polyglot.adapter;

import no.ssb.saga.execution.adapter.Adapter;
import org.json.JSONObject;

import java.util.UUID;

public class PublishToPubSub extends Adapter<JSONObject, JSONObject> {

    public static final String NAME = "PubSub";

    public PublishToPubSub(Class<JSONObject> outputClazz) {
        super(outputClazz, NAME);
    }

    @Override
    public JSONObject executeAction(JSONObject inputRoot) {
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
        return root;
    }

    @Override
    public JSONObject executeCompensatingAction(JSONObject inputRoot) {
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
        return root;
    }
}
