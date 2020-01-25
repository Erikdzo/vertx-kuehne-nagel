package com.erikdzo.vertx_kuehne_nagel;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class WebClientVerticle extends AbstractVerticle {

  private String url;

  public WebClientVerticle(String url) {
    this.url = url;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    System.out.printf("Requesting %s...\n", this.url);

    WebClient.create(vertx)
      .getAbs(this.url)
      .send(ar -> {
          JsonObject jsonResult = new JsonObject();
          EventBus eventBus = vertx.eventBus();

          jsonResult.put("url", this.url);

          if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            String responseBody = response.bodyAsString();

            System.out.printf("Received response from %s with status code %d\n", this.url, response.statusCode());

            jsonResult.put("success", true).put("bodySize", responseBody.length());
            eventBus.send("succeeded", jsonResult);

            startPromise.complete();
          } else {
            System.out.printf("Something went wrong: %s\n", ar.cause().getMessage());

            jsonResult.put("success", false);
            eventBus.send("failed", jsonResult);

            startPromise.fail(String.format("Requesting %s failed", this.url));
          }
        }
      );
  }

  @Override
  public void stop() {
    System.out.println("Stopping WebClientVerticle...");
  }
}
