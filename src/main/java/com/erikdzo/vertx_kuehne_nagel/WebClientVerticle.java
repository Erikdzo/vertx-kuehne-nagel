package com.erikdzo.vertx_kuehne_nagel;

import com.erikdzo.vertx_kuehne_nagel.utils.EventAddress;
import com.erikdzo.vertx_kuehne_nagel.utils.ResultMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.VertxException;
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

    EventBus eventBus = vertx.eventBus();
    JsonObject jsonResult = new JsonObject();

    jsonResult.put(ResultMessage.URL, this.url);

    try {
      WebClient.create(vertx)
        .getAbs(this.url)
        .send(handler -> {
            if (handler.succeeded()) {
              HttpResponse<Buffer> response = handler.result();
              String responseBody = response.bodyAsString();

              System.out.printf("Received response from %s with status code %d\n", this.url, response.statusCode());

              jsonResult.put(ResultMessage.BODY_SIZE, responseBody.length());
              eventBus.send(EventAddress.REQUEST_SUCCESS, jsonResult);

              startPromise.complete();
            } else {
              System.out.printf("Something went wrong: %s\n", handler.cause().getMessage());
              eventBus.send(EventAddress.REQUEST_FAIL, jsonResult);
              startPromise.fail(String.format("Requesting %s failed", this.url));
            }
          }
        );
    } catch (VertxException e) {
      eventBus.send(EventAddress.REQUEST_FAIL, jsonResult);
      System.out.println(e.getMessage());
      startPromise.fail(e.getMessage());
    }
  }


}
