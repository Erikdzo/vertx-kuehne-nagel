package com.erikdzo.vertx_kuehne_nagel;

import com.erikdzo.vertx_kuehne_nagel.utils.EventAddress;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

  private static Future<String> deployWebClientVerticle(Vertx vertx, String url) {
    Promise<String> promise = Promise.promise();
    vertx.deployVerticle(new WebClientVerticle(url), promise);

    return promise.future();
  }

  @Override
  public void start() {

    List<String> urls = Arrays.asList(
      "https://www.youtube.com",
      "https://www.google.com/",
      "https://www.facebook.com/test/test",
      "https://jsonplaceholder.typicode.com/posts/1"
    );

    List<Future<String>> futureList = new ArrayList<>();

    vertx.deployVerticle(new ReportVerticle());
    urls.forEach(url -> futureList.add(deployWebClientVerticle(vertx, url)));

    CompositeFuture.join(new ArrayList<>(futureList))
      .onComplete(handler -> vertx.eventBus().request(EventAddress.REPORT, true, handleResponse()));
  }

  private Handler<AsyncResult<Message<Object>>> handleResponse() {
    return handler -> {
      if (handler.succeeded()) {
        System.out.println(handler.result().body());
      } else {
        System.out.println("Failed to get response");
      }

      closeApp();
    };
  }

  private void closeApp() {
    this.vertx.close(handler -> {
      if (handler.succeeded()) {
        System.out.println("Closed Vert.x app");
      } else {
        System.out.printf("Failed to close app %s", handler.cause());
      }
    });
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
}
