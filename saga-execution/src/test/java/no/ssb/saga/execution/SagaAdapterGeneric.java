package no.ssb.saga.execution;

import no.ssb.saga.api.SagaNode;
import no.ssb.saga.execution.adapter.Adapter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SagaAdapterGeneric extends Adapter<Object, Object> {

    public static final String NAME = "Generic";

    public SagaAdapterGeneric() {
        super(Object.class, NAME);
    }

    @Override
    public Object prepareInputFromDependees(Object startInput, Map<SagaNode, Object> dependeesOutput) {
        JSONObject result = new JSONObject();
        result.put("originalRequest", new JSONObject(String.valueOf(startInput)));
        JSONArray array = new JSONArray();
        result.put("dependees", array);
        for (Map.Entry<SagaNode, Object> e : dependeesOutput.entrySet()) {
            JSONObject jo = new JSONObject();
            jo.put("node-id", e.getKey().id);
            Object value = e.getValue();
            jo.put("result", value == null ? null : new JSONObject(String.valueOf(value)));
            array.put(jo);
        }
        return result.toString();
    }

    @Override
    public Object executeAction(Object input) {
        JSONObject result = new JSONObject();
        result.put("action", "Generic Action Execution");
        result.put("unique-id", UUID.randomUUID().toString());
        result.put("response-code", 200);
        result.put("running-time-ms", new Random().nextInt(500));
        return result.toString();
    }

    @Override
    public Object executeCompensatingAction(Object input) {
        JSONObject result = new JSONObject();
        result.put("compensating action", "Generic Compensating Action Execution");
        result.put("unique-id", UUID.randomUUID().toString());
        result.put("response-code", 200);
        result.put("running-time-ms", new Random().nextInt(500));
        return result.toString();
    }
}
