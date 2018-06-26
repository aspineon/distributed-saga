package no.ssb.saga.samples.polyglot.adapter;

import no.ssb.saga.execution.adapter.SagaAdapter;
import no.ssb.saga.execution.adapter.VisitationResult;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class AdapterRDBMS implements SagaAdapter {

    public static final String NAME = "RDBMS";

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
        output.put("action", "Add data record to table X");
        output.put("SQL", "INSERT INTO X(column1, column2) VALUES('foo', '" + UUID.randomUUID() + "');");
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
        output.put("compensating action", "Delete data record from table X");
        output.put("SQL", "DELETE X WHERE column1 = 'foo' AND column2 = '" + UUID.randomUUID() + "';");
        output.put("txid", UUID.randomUUID());
        root.put("output", output);
        return root.toString();
    }
}
