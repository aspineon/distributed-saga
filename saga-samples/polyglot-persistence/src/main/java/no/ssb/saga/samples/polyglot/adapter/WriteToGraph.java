package no.ssb.saga.samples.polyglot.adapter;

import no.ssb.saga.execution.adapter.Adapter;
import org.json.JSONObject;

import java.util.UUID;

public class WriteToGraph extends Adapter<JSONObject, JSONObject> {

    public static final String NAME = "Graph";

    public WriteToGraph(Class<JSONObject> outputClazz) {
        super(outputClazz, NAME);
    }

    @Override
    public JSONObject executeAction(JSONObject inputRoot) {
        JSONObject root = new JSONObject();
        root.put("input", inputRoot);
        JSONObject output = new JSONObject();
        output.put("action", "Create NODES and LINKS");
        output.put("GraphOp", "CREATE Node('" + UUID.randomUUID() + "'); CREATE Link from recent node to Node('" + UUID.randomUUID() + "');");
        output.put("txid", UUID.randomUUID());
        root.put("output", output);
        return root;
    }

    @Override
    public JSONObject executeCompensatingAction(JSONObject inputRoot) {
        JSONObject root = new JSONObject();
        root.put("input", inputRoot);
        JSONObject output = new JSONObject();
        output.put("compensating action", "Mark NODES and related LINKS as removed");
        output.put("GraphOp", "UPDATE Node('" + UUID.randomUUID() + "') SET field removed=true;");
        output.put("txid", UUID.randomUUID());
        root.put("output", output);
        return root;
    }
}
