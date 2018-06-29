package no.ssb.saga.samples.polyglot.adapter;

import no.ssb.saga.execution.adapter.Adapter;
import org.json.JSONObject;

import java.util.UUID;

public class WriteToRDBMS extends Adapter<JSONObject, JSONObject> {

    public static final String NAME = "RDBMS";

    public WriteToRDBMS(Class<JSONObject> outputClazz) {
        super(outputClazz, NAME);
    }

    @Override
    public JSONObject executeAction(JSONObject inputRoot) {
        JSONObject root = new JSONObject();
        root.put("input", inputRoot);
        JSONObject output = new JSONObject();
        output.put("action", "Add data record to table X");
        output.put("SQL", "INSERT INTO X(column1, column2) VALUES('foo', '" + UUID.randomUUID() + "');");
        output.put("txid", UUID.randomUUID());
        root.put("output", output);
        return root;
    }

    @Override
    public JSONObject executeCompensatingAction(JSONObject inputRoot) {
        JSONObject root = new JSONObject();
        root.put("input", inputRoot);
        JSONObject output = new JSONObject();
        output.put("compensating action", "Delete data record from table X");
        output.put("SQL", "DELETE X WHERE column1 = 'foo' AND column2 = '" + UUID.randomUUID() + "';");
        output.put("txid", UUID.randomUUID());
        root.put("output", output);
        return root;
    }
}
