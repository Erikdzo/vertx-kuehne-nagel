package com.erikdzo.vertx_kuehne_nagel;

import io.vertx.core.*;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.util.*;

public class MainVerticle extends AbstractVerticle {

  private Map<String, String> requestResultsMap;

  public MainVerticle() {
    this.requestResultsMap = new HashMap<>();
  }

  private static Future<String> deploy(Vertx vertx, Verticle verticle) {
    Promise<String> promise = Promise.promise();
    vertx.deployVerticle(verticle, promise);

    return promise.future();
  }

  @Override
  public void start(Promise startPromise) {

    List<Future<String>> futureList = Arrays.asList(
      deploy(vertx, new WebpageVerticle("youtube.com")),
      deploy(vertx, new WebpageVerticle("google.com")),
      deploy(vertx, new WebpageVerticle("reddit.com")),
      deploy(vertx, new WebpageVerticle("facebook"))
    );

    MessageConsumer<JsonObject> msg = vertx.eventBus().consumer("report");

    msg.handler(ar -> requestResultsMap.put(ar.body().getString("url"), ar.body().getString("body")));

    CompositeFuture.join(new ArrayList<>(futureList))
      .setHandler(ar -> {
        if (ar.failed()) {
          System.out.println("1 or more web requests failed");
        }
        report();

        vertx.deploymentIDs().forEach(id -> vertx.undeploy(id));
      });

  }

  private void report() {

    System.out.println("REPORT:");

    requestResultsMap.forEach((k, v) -> System.out.println("PAGE: " + k + " SIZE (bytes): " + v.length()));

    int totalBytes = requestResultsMap.values().stream().mapToInt(String::length).sum();
    System.out.println("TOTAL SIZE (bytes): " + totalBytes);
  }
}
