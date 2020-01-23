package com.erikdzo.vertx_kuehne_nagel;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class WebpageVerticle extends AbstractVerticle {

    private String url;

    public WebpageVerticle(String url) {
        this.url = url;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        System.out.println("Requesting " + this.url);

        WebClient.create(vertx)
            .get(this.url, "")
            .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();

                        System.out.println("Received response from " + this.url + " with status code " + response.statusCode());

                        JsonObject json = new JsonObject().put("url", this.url).put("body", response.bodyAsString());
                        vertx.eventBus().send("report", json);

                        startPromise.complete();
                    } else {
                        System.out.println("Something went wrong: " + ar.cause().getMessage());
                        startPromise.fail("Something went wrong: " + ar.cause().getMessage());
                    }
                }
            );
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Stopping WebpageVerticle...");
    }
}
