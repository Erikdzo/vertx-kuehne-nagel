package com.erikdzo.vertx_kuehne_nagel;

import io.vertx.core.*;

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
    vertx.deployVerticle(new ReportVerticle());

    List<Future<String>> futureList = Arrays.asList(
      deployWebClientVerticle(vertx, "https://www.youtube.com/"),
      deployWebClientVerticle(vertx, "https://www.google.com/"),
      deployWebClientVerticle(vertx, "https://www.face"),
      deployWebClientVerticle(vertx, "https://jsonplaceholder.typicode.com/posts/1")
    );

    CompositeFuture.join(new ArrayList<>(futureList))
      .onComplete(handler ->
        vertx.eventBus().request("report", true, ar -> {
          if (ar.succeeded()) {
            System.out.println(ar.result().body());
          }
        }));
  }
}
