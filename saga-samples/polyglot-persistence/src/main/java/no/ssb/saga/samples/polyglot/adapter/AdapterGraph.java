package no.ssb.saga.samples.polyglot.adapter;

import no.ssb.saga.execution.adapter.SagaAdapter;
import no.ssb.saga.execution.adapter.VisitationResult;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class AdapterGraph implements SagaAdapter {

    public static final String NAME = "Graph";

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
        output.put("action", "Create NODES and LINKS");
        output.put("GraphOp", "CREATE Node('" + UUID.randomUUID() + "'); CREATE Link from recent node to Node('" + UUID.randomUUID() + "');");
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
        output.put("compensating action", "Mark NODES and related LINKS as removed");
        output.put("GraphOp", "UPDATE Node('" + UUID.randomUUID() + "') SET field removed=true;");
        output.put("txid", UUID.randomUUID());
        root.put("output", output);
        return root.toString();
    }
}
