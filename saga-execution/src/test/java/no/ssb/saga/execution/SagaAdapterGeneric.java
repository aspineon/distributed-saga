package no.ssb.saga.execution;

import no.ssb.saga.execution.adapter.SagaAdapter;
import no.ssb.saga.execution.adapter.VisitationResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class SagaAdapterGeneric implements SagaAdapter {

    public static final String NAME = "Generic";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String prepareJsonInputFromDependees(String originalRequestJson, List<VisitationResult<String>> dependeesOutput) {
        JSONObject result = new JSONObject();
        result.put("originalRequest", new JSONObject(originalRequestJson));
        JSONArray array = new JSONArray();
        result.put("dependees", array);
        for (VisitationResult<String> vr : dependeesOutput) {
            JSONObject jo = new JSONObject();
            jo.put("node-id", vr.node.id);
            jo.put("result", new JSONObject(vr.result));
            array.put(jo);
        }
        return result.toString();
    }

    @Override
    public String executeAction(String inputJson) {
        JSONObject result = new JSONObject();
        result.put("action", "Generic Action Execution");
        result.put("unique-id", UUID.randomUUID().toString());
        result.put("response-code", 200);
        result.put("running-time-ms", new Random().nextInt(500));
        return result.toString();
    }

    @Override
    public String executeCompensatingAction(String inputJson) {
        JSONObject result = new JSONObject();
        result.put("compensating action", "Generic Compensating Action Execution");
        result.put("unique-id", UUID.randomUUID().toString());
        result.put("response-code", 200);
        result.put("running-time-ms", new Random().nextInt(500));
        return result.toString();
    }
}
