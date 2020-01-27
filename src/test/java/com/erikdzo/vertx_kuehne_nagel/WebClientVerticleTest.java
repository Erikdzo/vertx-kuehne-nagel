package com.erikdzo.vertx_kuehne_nagel;

import com.erikdzo.vertx_kuehne_nagel.utils.EventAddress;
import com.erikdzo.vertx_kuehne_nagel.utils.ResultMessage;
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
  public void correctUrlVerticlePromiseComplete(Vertx vertx, VertxTestContext testContext) {
    stubFor(get("/test").willReturn(aResponse().withBody("test")));

    WebClientVerticle webClientVerticle = new WebClientVerticle("http://localhost:8080/test");
    vertx.deployVerticle(webClientVerticle, handler -> {
      testContext.verify(() -> assertTrue(handler.succeeded()));
      testContext.completeNow();
    });
  }

  @Test
  public void brokenUrlRequestVerticlePromiseFailed(Vertx vertx, VertxTestContext testContext) {
    WebClientVerticle webClientVerticle = new WebClientVerticle("http://localhos");
    vertx.deployVerticle(webClientVerticle, handler -> {
      testContext.verify(() -> assertTrue(handler.failed()));
      testContext.completeNow();
    });
  }

  @Test
  public void nonUrlRequestVerticlePromiseFailed(Vertx vertx, VertxTestContext testContext) {
    WebClientVerticle webClientVerticle = new WebClientVerticle("test");
    vertx.deployVerticle(webClientVerticle, handler -> {
      testContext.verify(() -> assertTrue(handler.failed()));
      testContext.completeNow();
    });
  }

  @Test
  public void invalidPathVerticlePromiseFailed(Vertx vertx, VertxTestContext testContext) {
    WebClientVerticle webClientVerticle = new WebClientVerticle("http://localhost:8080/test");
    vertx.deployVerticle(webClientVerticle, handler -> {
      testContext.verify(() -> assertTrue(handler.failed()));
      testContext.completeNow();
    });
  }

  @Test
  public void nullUrlRequestVerticlePromiseFailed(Vertx vertx, VertxTestContext testContext) {
    stubFor(get("/test").willReturn(aResponse().withBody("test")));

    WebClientVerticle webClientVerticle = new WebClientVerticle(null);
    vertx.deployVerticle(webClientVerticle, handler -> {
      testContext.verify(() -> assertTrue(handler.failed()));
      testContext.completeNow();
    });
  }
  @Test
  public void nullURLRequestSendFailedMessage(Vertx vertx, VertxTestContext testContext) {
    WebClientVerticle webClientVerticle = new WebClientVerticle(null);
    vertx.deployVerticle(webClientVerticle);

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(EventAddress.REQUEST_FAIL);

    consumer.handler(message -> testContext.completeNow());
  }

  @Test
  public void plainStringRequestSendsFailedMessage(Vertx vertx, VertxTestContext testContext) {
    WebClientVerticle webClientVerticle = new WebClientVerticle("test string");
    vertx.deployVerticle(webClientVerticle);

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(EventAddress.REQUEST_FAIL);

    consumer.handler(message -> testContext.completeNow());
  }

  @Test
  public void invalidPathURLRequestSendsFailedMessage(Vertx vertx, VertxTestContext testContext) {
    WebClientVerticle webClientVerticle = new WebClientVerticle("http://localhost:8080/test");
    vertx.deployVerticle(webClientVerticle);

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(EventAddress.REQUEST_FAIL);

    consumer.handler(message -> testContext.completeNow());
  }

  @Test
  @Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
  public void successfulRequestSendsEventBusMessage(Vertx vertx, VertxTestContext testContext) {

    stubFor(get("/test").willReturn(aResponse().withBody("test")));

    WebClientVerticle webClientVerticle = new WebClientVerticle("http://localhost:8080/test");
    vertx.deployVerticle(webClientVerticle);

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(EventAddress.REQUEST_SUCCESS);

    consumer.handler(message -> {
      testContext.verify(() -> {
        assertTrue(message.body().containsKey(ResultMessage.URL));
        assertTrue(message.body().containsKey(ResultMessage.BODY_SIZE));
        assertFalse(message.body().containsKey(ResultMessage.ERROR));
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

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(EventAddress.REQUEST_SUCCESS);

    consumer.handler(message -> {
      testContext.verify(() -> assertEquals(url, message.body().getString(ResultMessage.URL)));
      testContext.completeNow();
    });
  }

  @Test
  public void successfulRequestMessageCorrectBodySize(Vertx vertx, VertxTestContext testContext) {
    String body = "test";

    stubFor(get("/test").willReturn(aResponse().withBody(body)));

    WebClientVerticle webClientVerticle = new WebClientVerticle("http://localhost:8080/test");
    vertx.deployVerticle(webClientVerticle);

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(EventAddress.REQUEST_SUCCESS);

    consumer.handler(message -> {
      testContext.verify(() -> assertEquals(body.length(), message.body().getInteger(ResultMessage.BODY_SIZE)));
      testContext.completeNow();
    });
  }

  @Test
  public void failedRequestSendEventBusMessage(Vertx vertx, VertxTestContext testContext) {
    WebClientVerticle webClientVerticle = new WebClientVerticle("http://localhos");
    vertx.deployVerticle(webClientVerticle);

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(EventAddress.REQUEST_FAIL);

    consumer.handler(message -> {
      testContext.verify(() -> {
        assertTrue(message.body().containsKey(ResultMessage.URL));
        assertTrue(message.body().containsKey(ResultMessage.ERROR));
        assertFalse(message.body().containsKey(ResultMessage.BODY_SIZE));
      });
      testContext.completeNow();
    });
  }

  @Test
  public void failedRequestMessageCorrectURL(Vertx vertx, VertxTestContext testContext) {
    String url = "http://localhos";

    WebClientVerticle webClientVerticle = new WebClientVerticle(url);
    vertx.deployVerticle(webClientVerticle);

    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(EventAddress.REQUEST_FAIL);

    consumer.handler(message -> {
      testContext.verify(() -> assertEquals(url, message.body().getString(ResultMessage.URL)));
      testContext.completeNow();
    });
  }
}
