package com.erikdzo.vertx_kuehne_nagel;

import com.erikdzo.vertx_kuehne_nagel.utils.EventAddress;
import com.erikdzo.vertx_kuehne_nagel.utils.ResultMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ReportVerticle extends AbstractVerticle {

  private List<JsonObject> succeededRequestList;
  private List<JsonObject> failedRequestsList;

  public ReportVerticle() {
    this.succeededRequestList = new ArrayList<>();
    this.failedRequestsList = new ArrayList<>();
  }

  @Override
  public void start() {

    MessageConsumer<JsonObject> successConsumer = vertx.eventBus().consumer(EventAddress.REQUEST_SUCCESS);
    MessageConsumer<JsonObject> failConsumer = vertx.eventBus().consumer(EventAddress.REQUEST_FAIL);
    MessageConsumer<JsonObject> reportConsumer = vertx.eventBus().consumer(EventAddress.REPORT);

    successConsumer.handler(message -> succeededRequestList.add(message.body()));
    failConsumer.handler(message -> failedRequestsList.add(message.body()));
    reportConsumer.handler(message -> message.reply(report()));
  }

  private String report() {
    String report = "";

    if (succeededRequestList.isEmpty() && failedRequestsList.isEmpty()) {
      report += reportHeader();
      report += "Nothing to report\n";
      report += reportFooter();
    } else {
      report += reportHeader();
      report += reportContent();
      report += reportFooter();
    }

    return report;
  }

  private String reportHeader() {
    return String.format("%s\n%s\n", "REPORT", "----------------------------------------");
  }

  private String reportContent() {
    String content = "";

    content += String.format("%d requests succeeded %d failed\n", succeededRequestList.size(), failedRequestsList.size());

    // Display each web page request results categorized by success
    if (!succeededRequestList.isEmpty()) {
      content += "SUCCEEDED:\n";
      content += succeededRequestList.stream().map(this::formatSucceededRequest).reduce("", String::concat);
    }
    if (!failedRequestsList.isEmpty()) {
      content += "FAILED:\n";
      content += failedRequestsList.stream().map(this::formatFailedRequest).reduce("", String::concat);
    }

    int total = succeededRequestList.stream()
      .map(message -> message.getInteger(ResultMessage.BODY_SIZE))
      .reduce(0, Integer::sum);


    long avg = total;

    if (!succeededRequestList.isEmpty()) {
      avg /= succeededRequestList.size();
    }

    content += String.format("TOTAL SIZE (bytes): %d\n", total);
    content += String.format("AVERAGE SIZE (bytes): %d\n", avg);

    return content;
  }

  private String formatSucceededRequest(JsonObject requestJson) {
    return String.format("URL: %s SIZE (bytes): %d\n", requestJson.getString(ResultMessage.URL), requestJson.getInteger(ResultMessage.BODY_SIZE));
  }

  private String formatFailedRequest(JsonObject requestJson) {
    return String.format("URL: %s CAUSE: [%s]\n", requestJson.getString(ResultMessage.URL), requestJson.getString("error"));
  }

  private String reportFooter() {
    return "----------------------------------------";
  }
}
