package no.ssb.saga.samples.polyglot;

import io.undertow.io.Receiver;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import no.ssb.concurrent.futureselector.SelectableThreadPoolExectutor;
import no.ssb.saga.api.Saga;
import no.ssb.saga.execution.SagaExecution;
import no.ssb.saga.execution.adapter.AdapterLoader;
import no.ssb.saga.execution.sagalog.SagaLog;
import no.ssb.saga.samples.polyglot.adapter.AdapterGraph;
import no.ssb.saga.samples.polyglot.adapter.AdapterObjectStore;
import no.ssb.saga.samples.polyglot.adapter.AdapterPubSub;
import no.ssb.saga.samples.polyglot.adapter.AdapterRDBMS;
import no.ssb.saga.serialization.SagaSerializer;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PolyglotHttpHandler implements HttpHandler {
    private final SelectableThreadPoolExectutor executorService;

    public PolyglotHttpHandler(SelectableThreadPoolExectutor executorService) {
        this.executorService = executorService;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        if (exchange.getRequestMethod().equalToString("get")) {
            return;
        } else if (exchange.getRequestMethod().equalToString("put")) {
            exchange.getRequestReceiver().receiveFullBytes(PUTCallback());
            return;
        }

        exchange.setStatusCode(400);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("Unsupported method: " + exchange.getRequestMethod());
    }

    private Receiver.FullBytesCallback PUTCallback() {
        return (exchange, buf) -> {


            /*
             * Register all adapters/drivers that will be used in saga.
             */
            AdapterLoader adapterLoader = new AdapterLoader()
                    .register(new AdapterRDBMS())
                    .register(new AdapterGraph())
                    .register(new AdapterObjectStore())
                    .register(new AdapterPubSub());


            /*
             * Create Saga that will first write to 3 different persistence-technologies
             * in parallel, then publish an event to a pub-sub technology.
             */
            Saga polyglotSaga = Saga.start("Polyglot Saga")
                    .linkTo("rdbms", "graph", "objectstore")
                    .id("rdbms").adapter(AdapterRDBMS.NAME).linkTo("pubsub")
                    .id("graph").adapter(AdapterGraph.NAME).linkTo("pubsub")
                    .id("objectstore").adapter(AdapterObjectStore.NAME).linkTo("pubsub")
                    .id("pubsub").adapter(AdapterPubSub.NAME).linkToEnd()
                    .end();


            /*
             * In a real-world application you would typically replace this echo SagaLog
             * with an integration with an external replicated log.
             */
            Deque<String> logEntries = new ConcurrentLinkedDeque<>();
            SagaLog sagaLog = (node, data) -> {
                logEntries.addLast(JSONObject.quote(data));
                return "{\"logid\":\"" + UUID.randomUUID() + "\"}";
            };


            /*
             * Prepare Saga input
             */
            JSONObject inputRoot = new JSONObject();
            inputRoot.put("resource-path", exchange.getRequestPath());
            inputRoot.put("data", new String(buf, StandardCharsets.UTF_8));
            String sagaInputStr = inputRoot.toString();


            /*
             * Execute Saga
             */
            SagaExecution sagaExecution = new SagaExecution(sagaLog, executorService, polyglotSaga, adapterLoader);
            boolean success = false;
            try {
                sagaExecution.executeSaga(sagaInputStr, 5, TimeUnit.MINUTES);
                success = true;
            } finally {
                if (!success) {
                    sagaExecution.rollbackSaga(sagaInputStr, 5, TimeUnit.MINUTES);
                }
            }


            /*
             * Respond with result
             */
            exchange.setStatusCode(201);
            exchange.getResponseHeaders().put(new HttpString("Content-Type"), "application/json; charset=utf-8");
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"saga\":").append(SagaSerializer.toJson(polyglotSaga));
            sb.append(",");
            sb.append("\"log\":[");
            sb.append(logEntries.stream().collect(Collectors.joining(",")));
            sb.append("]");
            sb.append("}");
            ByteBuffer responseData = ByteBuffer.wrap(sb.toString().getBytes(StandardCharsets.UTF_8));
            exchange.setResponseContentLength(responseData.limit());
            exchange.getResponseSender().send(responseData);
        };
    }
}
