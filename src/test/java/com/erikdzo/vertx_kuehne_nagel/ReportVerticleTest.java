package com.erikdzo.vertx_kuehne_nagel;

import com.erikdzo.vertx_kuehne_nagel.utils.EventAddress;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
class ReportVerticleTest {

  WireMockServer wireMockServer;

  @BeforeEach
  void startMockServer() {
    wireMockServer = new WireMockServer(8080);
    wireMockServer.start();
  }

  @AfterEach
  void stopMockServer() {
    wireMockServer.stop();
  }

  @Test
  public void reportCorrectWithOneSuccessfulRequest(Vertx vertx, VertxTestContext testContext) {
    stubFor(get("/test").willReturn(aResponse().withBody("test")));
    String expected = "REPORT\n" +
      "----------------------------------------\n" +
      "1 requests succeeded 0 failed\n" +
      "SUCCEEDED:\n" +
      "URL: http://localhost:8080/test SIZE (bytes): 4\n" +
      "TOTAL SIZE (bytes): 4\n" +
      "AVERAGE SIZE (bytes): 4\n" +
      "----------------------------------------";

    WebClientVerticle webClientVerticle = new WebClientVerticle("http://localhost:8080/test");
    ReportVerticle reportVerticle = new ReportVerticle();

    vertx.deployVerticle(reportVerticle);
    vertx.deployVerticle(webClientVerticle, handler ->
      vertx.eventBus().request(EventAddress.REPORT, true, responseHandler -> {
        testContext.verify(() -> assertEquals(expected, responseHandler.result().body()));
        testContext.completeNow();
      }));
  }

  @Test
  public void reportEmptyWhenNoRequestsMade(Vertx vertx, VertxTestContext testContext) {

    ReportVerticle reportVerticle = new ReportVerticle();

    String expected = "REPORT\n" +
      "----------------------------------------\n" +
      "Nothing to report\n" +
      "----------------------------------------";

    vertx.deployVerticle(reportVerticle);

    vertx.eventBus().request(EventAddress.REPORT, true, handler -> {
      testContext.verify(() -> assertEquals(expected, handler.result().body()));
      testContext.completeNow();
    });
  }

  @Test
  public void reportCorrectWithOneSuccessfulAndOneFailedRequest(Vertx vertx, VertxTestContext testContext) {
    String expected = "REPORT\n" +
      "----------------------------------------\n" +
      "1 requests succeeded 1 failed\n" +
      "SUCCEEDED:\n" +
      "URL: http://localhost:8080/test SIZE (bytes): 4\n" +
      "FAILED:\n" +
      "URL: http://loca CAUSE: [failed to resolve 'loca' after 3 queries ]\n" +
      "TOTAL SIZE (bytes): 4\n" +
      "AVERAGE SIZE (bytes): 4\n" +
      "----------------------------------------";
    stubFor(get("/test").willReturn(aResponse().withBody("test")));

    ReportVerticle reportVerticle = new ReportVerticle();

    vertx.deployVerticle(reportVerticle);
    CompositeFuture.join(
      deployWebClientVerticle(vertx, "http://localhost:8080/test"),
      deployWebClientVerticle(vertx, "http://loca"))
      .onComplete(handler ->
        vertx.eventBus().request(EventAddress.REPORT, true, responseHandler -> {
          testContext.verify(() -> assertEquals(expected, responseHandler.result().body()));
          testContext.completeNow();
        }));
  }

  @Test
  public void reportCorrectWithTwoSuccessfulRequest(Vertx vertx, VertxTestContext testContext) {
    String expected = "REPORT\n" +
      "----------------------------------------\n" +
      "2 requests succeeded 0 failed\n" +
      "SUCCEEDED:\n" +
      "URL: http://localhost:8080/test SIZE (bytes): 4\n" +
      "URL: http://localhost:8080/test SIZE (bytes): 4\n" +
      "TOTAL SIZE (bytes): 8\n" +
      "AVERAGE SIZE (bytes): 4\n" +
      "----------------------------------------";
    stubFor(get("/test").willReturn(aResponse().withBody("test")));

    ReportVerticle reportVerticle = new ReportVerticle();

    vertx.deployVerticle(reportVerticle);
    CompositeFuture.join(
      deployWebClientVerticle(vertx, "http://localhost:8080/test"),
      deployWebClientVerticle(vertx, "http://localhost:8080/test"))
      .onComplete(handler ->
        vertx.eventBus().request(EventAddress.REPORT, true, responseHandler -> {
          testContext.verify(() -> assertEquals(expected, responseHandler.result().body()));
          testContext.completeNow();
        }));
  }

  @Test
  public void reportCorrectWithTwoFailedRequest(Vertx vertx, VertxTestContext testContext) {
    String expected = "REPORT\n" +
      "----------------------------------------\n" +
      "0 requests succeeded 2 failed\n" +
      "FAILED:\n" +
      "URL: asd wdapwmd CAUSE: [Invalid url: asd wdapwmd]\n" +
      "URL: http://local CAUSE: [failed to resolve 'local' after 3 queries ]\n" +
      "TOTAL SIZE (bytes): 0\n" +
      "AVERAGE SIZE (bytes): 0\n" +
      "----------------------------------------";

    ReportVerticle reportVerticle = new ReportVerticle();

    vertx.deployVerticle(reportVerticle);
    CompositeFuture.join(
      deployWebClientVerticle(vertx, "http://local"),
      deployWebClientVerticle(vertx, "asd wdapwmd"))
      .onComplete(handler ->
        vertx.eventBus().request(EventAddress.REPORT, true, responseHandler -> {
          testContext.verify(() -> assertEquals(expected, responseHandler.result().body()));
          testContext.completeNow();
        }));
  }

  private static Future<String> deployWebClientVerticle(Vertx vertx, String url) {
    Promise<String> promise = Promise.promise();
    vertx.deployVerticle(new WebClientVerticle(url), promise);

    return promise.future();
  }
}
