package com.erikdzo.vertx_kuehne_nagel;

import com.erikdzo.vertx_kuehne_nagel.utils.EventAddress;
import com.erikdzo.vertx_kuehne_nagel.utils.ResultMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;
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

    try {
      WebClient.create(vertx)
        .getAbs(this.url)
        .send(handler -> {
            if (handler.succeeded()) {
              HttpResponse<Buffer> response = handler.result();
              if (response.statusCode() == 200) {
                handleSuccess(response);
                startPromise.complete();
              } else {
                handleFailure(response.statusMessage());
                startPromise.fail(String.format("Invalid status code from %s", this.url));
              }
            } else {
              handleFailure(handler.cause().getMessage());
              startPromise.fail(String.format("Requesting %s failed", this.url));
            }
          }
        );
    } catch (VertxException e) {
      handleFailure(e.getMessage());
      startPromise.fail(String.format("WebClient failed: %s", e.getMessage()));
    }
  }

  public void handleSuccess(HttpResponse<Buffer> response) {
    System.out.printf("Received response from %s with status code %d\n", this.url, response.statusCode());
    JsonObject json = new JsonObject();

    json
      .put(ResultMessage.URL, this.url)
      .put(ResultMessage.BODY_SIZE, response.body().length());

    vertx.eventBus().send(EventAddress.REQUEST_SUCCESS, json);
  }

  public void handleFailure(String msg) {
    System.out.printf("%s failed: %s\n", this.url, msg);

    JsonObject json = new JsonObject();

    json
      .put(ResultMessage.URL, this.url)
      .put(ResultMessage.ERROR, msg);

    vertx.eventBus().send(EventAddress.REQUEST_FAIL, json);
  }

}
