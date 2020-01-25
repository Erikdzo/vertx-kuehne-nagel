package com.erikdzo.vertx_kuehne_nagel;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class WebClientVerticleTest {

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
  public void successfulRequestPromiseComplete(Vertx vertx, VertxTestContext testContext) {
    stubFor(get("/test").willReturn(aResponse().withBody("test")));

    WebClientVerticle webClientVerticle = new WebClientVerticle("http://localhost:8080/test");
    vertx.deployVerticle(webClientVerticle, handler -> {
      testContext.verify(() -> assertTrue(handler.succeeded()));
      testContext.completeNow();
    });
  }

  @Test
  @Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
  public void successfulRequestSendEventBusMessage(Vertx vertx, VertxTestContext testContext) {

    stubFor(get("/test").willReturn(aResponse().withBody("test")));

    WebClientVerticle webClientVerticle = new WebClientVerticle("http://localhost:8080/test");
    vertx.deployVerticle(webClientVerticle);

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("succeeded");

    consumer.handler(message -> {
      testContext.verify(() -> {
        assertTrue(message.body().containsKey("url"));
        assertTrue(message.body().containsKey("bodySize"));
      });
      testContext.completeNow();
    });
  }

  @Test
  public void successfulRequestMessageCorrectURL(Vertx vertx, VertxTestContext testContext) {
    String url = "http://localhost:8080/test";

    stubFor(get("/test").willReturn(aResponse().withBody("test")));

    WebClientVerticle webClientVerticle = new WebClientVerticle(url);
    vertx.deployVerticle(webClientVerticle);

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("succeeded");

    consumer.handler(message -> {
      testContext.verify(() -> assertEquals(url, message.body().getString("url")));
      testContext.completeNow();
    });
  }

  @Test
  public void successfulRequestMessageCorrectBodySize(Vertx vertx, VertxTestContext testContext) {
    String body = "test";

    stubFor(get("/test").willReturn(aResponse().withBody(body)));

    WebClientVerticle webClientVerticle = new WebClientVerticle("http://localhost:8080/test");
    vertx.deployVerticle(webClientVerticle);

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("succeeded");

    consumer.handler(message -> {
      testContext.verify(() -> assertEquals(body.length(), message.body().getInteger("bodySize")));
      testContext.completeNow();
    });
  }

  @Test
  public void failedRequestPromiseComplete(Vertx vertx, VertxTestContext testContext) {
    stubFor(get("/test").willReturn(aResponse().withBody("test")));

    WebClientVerticle webClientVerticle = new WebClientVerticle("http://localhos");
    vertx.deployVerticle(webClientVerticle, handler -> {
      testContext.verify(() -> assertTrue(handler.failed()));
      testContext.completeNow();
    });
  }

  @Test
  public void failedRequestSendEventBusMessage(Vertx vertx, VertxTestContext testContext) {
    WebClientVerticle webClientVerticle = new WebClientVerticle("http://localhos");
    vertx.deployVerticle(webClientVerticle);

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("failed");

    consumer.handler(message -> {
      testContext.verify(() -> {
        assertTrue(message.body().containsKey("url"));
        assertFalse(message.body().containsKey("bodySize"));
      });
      testContext.completeNow();
    });
  }

  @Test
  public void failedRequestMessageCorrectURL(Vertx vertx, VertxTestContext testContext) {
    String url = "http://localhos";

    WebClientVerticle webClientVerticle = new WebClientVerticle(url);
    vertx.deployVerticle(webClientVerticle);

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("failed");

    consumer.handler(message -> {
      testContext.verify(() -> assertEquals(url, message.body().getString("url")));
      testContext.completeNow();
    });
  }
}
