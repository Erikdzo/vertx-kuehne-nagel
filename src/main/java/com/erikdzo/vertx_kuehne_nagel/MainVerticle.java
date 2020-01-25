package com.erikdzo.vertx_kuehne_nagel;

import io.vertx.core.*;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

  private List<JsonObject> succeededRequestList;
  private List<JsonObject> failedRequestsList;

  public MainVerticle() {
    this.succeededRequestList = new ArrayList<>();
    this.failedRequestsList = new ArrayList<>();
  }

  private static Future<String> deployWebClientVerticle(Vertx vertx, String url) {
    Promise<String> promise = Promise.promise();
    vertx.deployVerticle(new WebClientVerticle(url), promise);

    return promise.future();
  }

  @Override
  public void start() {

    List<Future<String>> futureList = Arrays.asList(
      deployWebClientVerticle(vertx, "https://www.youtube.com/"),
      deployWebClientVerticle(vertx, "https://www.google.com/"),
      deployWebClientVerticle(vertx, "https://www.face"),
      deployWebClientVerticle(vertx, "https://jsonplaceholder.typicode.com/posts/1")
    );

    MessageConsumer<JsonObject> successConsumer = vertx.eventBus().consumer("succeeded");
    MessageConsumer<JsonObject> failureConsumer = vertx.eventBus().consumer("failed");

    successConsumer.handler(message -> succeededRequestList.add(message.body()));
    failureConsumer.handler(message -> failedRequestsList.add(message.body()));

    CompositeFuture.join(new ArrayList<>(futureList))
      .onComplete(handler -> {
        System.out.println(report());

        vertx.deploymentIDs().forEach(id -> vertx.undeploy(id));
      });
  }

  private String report() {
    String report = "";

    report += reportHeader();
    report += reportContent();
    report += reportFooter();

    return report;
  }

  private String reportHeader() {
    return String.format("%s\n%s\n", "REPORT", "----------------------------------------");
  }

  private String reportContent() {
    String content = "";

    content += String.format("%d requests succeeded %d failed\n", succeededRequestList.size(), failedRequestsList.size());

    // Display each web page request results by success
    if (!succeededRequestList.isEmpty()) {
      content += "SUCCEEDED:\n";
      content += succeededRequestList.stream().map(this::formatSucceededRequest).reduce("", String::concat);
    }
    if (!failedRequestsList.isEmpty()) {
      content += "FAILED:\n";
      content += failedRequestsList.stream().map(this::formatFailedRequest).reduce("", String::concat);
    }

    int total = succeededRequestList.stream()
      .filter(message -> message.getBoolean("success"))
      .map(message -> message.getInteger("bodySize"))
      .reduce(0, Integer::sum);

    long avg = total / succeededRequestList.size();

    content += String.format("TOTAL SIZE (bytes): %d\n", total);
    content += String.format("AVERAGE SIZE (bytes): %d\n", avg);

    return content;
  }

  private String formatSucceededRequest(JsonObject requestJson) {
    return String.format("URL: %s SIZE (bytes): %d\n", requestJson.getString("url"), requestJson.getInteger("bodySize"));
  }

  private String formatFailedRequest(JsonObject requestJson) {
    return String.format("URL: %s\n", requestJson.getString("url"));
  }

  private String reportFooter() {
    return "----------------------------------------";
  }
}
