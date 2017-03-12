package com.ps;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;


public class VerticleExample extends AbstractVerticle {

    private WhiskyStorage storage;
    private JsonObject config;

    @Override
    public void start(Future<Void> fut) throws Exception {
        config = !config().isEmpty() ? config() : defaultConfig();
        storage = new WhiskyStorage(vertx, config);
        storage.startDb(connection -> {
            storage.createSomeData(connection, event -> {
                startWebApp(fut);
            }, fut);
        }, fut);
    }

    @SuppressWarnings("unchecked")
    private JsonObject defaultConfig() throws IOException, URISyntaxException {
        URL config = getClass().getClassLoader().getResource("application-conf.json");
        byte[] bytes = Files.readAllBytes(Paths.get(config.toURI()));
        Map<String, Object> map = Json.decodeValue(new String(bytes), Map.class);
        return new JsonObject(map);
    }

    private void startWebApp(Future<Void> fut) {
        Router router = Router.router(vertx);
        router.route("/").handler(context -> {
            HttpServerResponse response = context.response();
            response.putHeader("content-type", "text/html")
                    .end("<h1>Hello from my first Vert.x 3 application</h1>");
        });
        router.route("/assets/*").handler(StaticHandler.create("assets"));
        router.route("/api/whiskies*").handler(BodyHandler.create());
        router.post("/api/whiskies").handler(this::addOne);
        router.get("/api/whiskies").handler(this::getAll);
        router.get("/api/whiskies/:id").handler(this::getOne);
        router.put("/api/whiskies/:id").handler(this::updateOne);
        router.delete("/api/whiskies/:id").handler(this::deleteOne);
        // Start server
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(config().getInteger("http.port", 8080),
                        result -> {
                            if (result.succeeded()) {
                                fut.complete();
                            } else {
                                fut.fail(result.cause());
                            }
                        });
    }

    private void addOne(RoutingContext context) {
        Whisky whisky = Json.decodeValue(context.getBodyAsString(), Whisky.class);
        storage.insertOne(whisky, result -> {
            if (result.failed()) {
                context.response().setStatusCode(500).end(result.cause().getMessage());
                return;
            }
            context.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json")
                    .end(Json.encode(result.result()));
        });
    }

    private void getAll(RoutingContext context) {
        storage.getAll(result -> {
            if (result.failed()) {
                context.response().setStatusCode(500).end(result.cause().getMessage());
                return;
            }
            context.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encode(result.result()));
        });
    }

    private void getOne(RoutingContext context) {
        String id = context.request().getParam("id");
        if (id == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        storage.getOne(Integer.valueOf(id), result -> {
            if (result.failed()) {
                context.response().setStatusCode(500).end(result.cause().getMessage());
                return;
            }
            if (result.result() == null) {
                context.response().setStatusCode(404).end("Not found");
                return;
            }
            context.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encode(result.result()));
        });

    }

    private void updateOne(RoutingContext context) {
        String id = context.request().getParam("id");
        if (id == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        Whisky whisky = Json.decodeValue(context.getBodyAsString(), Whisky.class);
        whisky.setId(Integer.valueOf(id));
        storage.updateOne(whisky, result -> {
            if (result.failed()) {
                context.response().setStatusCode(500).end(result.cause().getMessage());
                return;
            }
            context.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encode(result.result()));
        });
    }

    private void deleteOne(RoutingContext context) {
        String id = context.request().getParam("id");
        if (id == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        storage.deleteOne(Integer.valueOf(id), result -> {
            if (result.failed()) {
                context.response().setStatusCode(500).end(result.cause().getMessage());
                return;
            }
            context.response().setStatusCode(204).end();
        });
    }

}
