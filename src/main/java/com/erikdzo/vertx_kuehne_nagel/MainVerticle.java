package com.erikdzo.vertx_kuehne_nagel;

import io.vertx.core.*;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

  private List<JsonObject> reportMessageList;

  public MainVerticle() {
    this.reportMessageList = new ArrayList<>();
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

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("report");

    consumer.handler(message -> reportMessageList.add(message.body()));

    CompositeFuture.join(new ArrayList<>(futureList))
      .onComplete(handler -> {
        report();

        vertx.deploymentIDs().forEach(id -> vertx.undeploy(id));
      });
  }

  private void report() {
    String report = "";

    report += reportHeader();
    report += reportContent();
    report += reportFooter();

    System.out.println(report);
  }

  private String reportHeader() {
    return String.format("%s\n%s\n", "REPORT", "----------------------------------------");
  }

  private String reportContent() {
    String content = "";

    long succeededCount = reportMessageList.stream()
      .filter(message -> message.getBoolean("success")).count();

    content += String.format("%d requests succeeded %d failed\n", succeededCount, reportMessageList.size() - succeededCount);

    // Display each web page request results
    content += reportMessageList.stream().map(this::reportMessageStr).reduce("", String::concat);

    int total = reportMessageList.stream()
      .filter(message -> message.getBoolean("success"))
      .map(message -> message.getInteger("bodySize"))
      .reduce(0, Integer::sum);

    long avg = total / succeededCount;

    content += String.format("TOTAL SIZE (bytes): %d\n", total);
    content += String.format("AVERAGE SIZE (bytes): %d\n", avg);
    return content;
  }

  private String reportMessageStr(JsonObject response) {
    boolean success = response.getBoolean("success");

    if (success) {
      return String.format("URL: %s SUCCESS: %b SIZE (bytes): %d\n", response.getString("url"), success, response.getInteger("bodySize"));
    } else {
      return String.format("URL: %s SUCCESS: %b\n", response.getString("url"), success);
    }
  }

  private String reportFooter() {
    return "----------------------------------------";
  }
}
