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
    System.out.println("Requesting " + this.url + "...");

    WebClient.create(vertx)
      .get(this.url, "")
      .send(ar -> {
          JsonObject jsonResult = new JsonObject();
          EventBus eventBus = vertx.eventBus();

          jsonResult.put("url", this.url);

          if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            String responseBody = response.bodyAsString();

            System.out.println("Received response from " + this.url + " with status code " + response.statusCode());
            jsonResult.put("success", true).put("body", responseBody).put("bodySize", responseBody.length());

            eventBus.send("report", jsonResult);

            startPromise.complete();
          } else {
            jsonResult.put("success", false);
            eventBus.send("report", jsonResult);

            System.out.println("Something went wrong: " + ar.cause().getMessage());
            startPromise.fail("Something went wrong: " + ar.cause().getMessage());
          }
        }
      );
  }

  @Override
  public void stop() {
    System.out.println("Stopping WebClientVerticle...");
  }
}
