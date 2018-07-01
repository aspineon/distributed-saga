package no.ssb.saga.samples.polyglot;

import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class PolyglotMainTest {

    @Test
    public void thatCoordinatorCanExecuteSaga() throws URISyntaxException, IOException, InterruptedException {
        PolyglotMain polyglotMain = new PolyglotMain(8342, "127.0.0.1", "target/saga.log").start();

        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest httpRequest = HttpRequest
                .newBuilder(new URI("http://127.0.0.1:8342/test1"))
                .header("Content-Type", "application/octet-stream")
                .PUT(HttpRequest.BodyPublisher.fromString("Hello Polyglot", StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandler.asString(StandardCharsets.UTF_8));
        Assert.assertEquals(response.statusCode(), 201);
        String json = response.body();
        System.out.println(new JSONObject(json).toString(2));

        polyglotMain.stop();
    }
}
