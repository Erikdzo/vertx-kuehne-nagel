package com.erikdzo.vertx_kuehne_nagel;

import io.vertx.core.*;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

  private List<JsonObject> resultList;

  public MainVerticle() {
    this.resultList = new ArrayList<>();
  }

  private static Future<String> deployWebClientVerticle(Vertx vertx, String url) {
    Promise<String> promise = Promise.promise();
    vertx.deployVerticle(new WebClientVerticle(url), promise);

    return promise.future();
  }

  @Override
  public void start(Promise startPromise) {
    List<Future<String>> futureList = Arrays.asList(
      deployWebClientVerticle(vertx, "youtube.com"),
      deployWebClientVerticle(vertx, "google.com"),
      deployWebClientVerticle(vertx, "reddit.com"),
      deployWebClientVerticle(vertx, "facebook.com")
    );

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("report");

    consumer.handler(ar -> resultList.add(ar.body()));

    CompositeFuture.join(new ArrayList<>(futureList))
      .setHandler(ar -> {
        report();

        vertx.deploymentIDs().forEach(id -> vertx.undeploy(id));
      });
  }

  private void report() {
    System.out.println("REPORT");
    System.out.println("----------------------------------------");

    long succeededCount = resultList.stream()
      .filter(r -> r.getBoolean("success")).count();

    System.out.println(succeededCount + " requests succeeded " + (resultList.size() - succeededCount) + " failed");
    System.out.println();

    // Display each web page request results
    resultList.forEach(r -> {
      boolean success = r.getBoolean("success");
      System.out.print("SUCCESS: " + success + " URL: " + r.getString("url"));
      if (success) {
        System.out.print(" SIZE (bytes): " + r.getInteger("bodySize"));
      }
      System.out.println();
    });

    System.out.println();

    int total = resultList.stream()
      .filter(r -> r.getBoolean("success"))
      .map(r -> r.getInteger("bodySize"))
      .reduce(0, Integer::sum);

    long avg = total / succeededCount;

    System.out.println("TOTAL SIZE (bytes): " + total);
    System.out.println("AVERAGE SIZE (bytes): " + avg);
    System.out.println("----------------------------------------");
  }
}
