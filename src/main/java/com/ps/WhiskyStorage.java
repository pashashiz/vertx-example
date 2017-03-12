package com.ps;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.util.List;
import java.util.stream.Collectors;

public class WhiskyStorage {

    private JDBCClient jdbc;

    public WhiskyStorage(Vertx vertx, JsonObject jdbcConfig) {
        jdbc = JDBCClient.createShared(vertx, jdbcConfig, "My-Whisky-Collection");
    }

    public void startDb(Handler<AsyncResult<SQLConnection>> next, Future<Void> fut) {
        jdbc.getConnection(ar -> {
            if (ar.failed()) {
                fut.fail(ar.cause());
            } else {
                next.handle(Future.succeededFuture(ar.result()));
            }
        });
    }

    public void createSomeData(AsyncResult<SQLConnection> result,
                                Handler<AsyncResult<Void>> next, Future<Void> fut) {
        if (result.failed()) {
            fut.fail(result.cause());
        } else {
            SQLConnection connection = result.result();
            connection.execute(
                    "CREATE TABLE IF NOT EXISTS Whisky (id INTEGER IDENTITY, name varchar(100), " +
                            "origin varchar(100))",
                    ar -> {
                        if (ar.failed()) {
                            fut.fail(ar.cause());
                            connection.close();
                            return;
                        }
                        connection.query("SELECT * FROM Whisky", select -> {
                            if (select.failed()) {
                                fut.fail(ar.cause());
                                connection.close();
                                return;
                            }
                            if (select.result().getNumRows() == 0) {
                                insertOneInsideConnection(
                                        new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay"),
                                        connection,
                                        (v) -> insertOneInsideConnection(new Whisky("Talisker 57° North", "Scotland, Island"),
                                                connection,
                                                (r) -> {
                                                    next.handle(Future.<Void>succeededFuture());
                                                    connection.close();
                                                }));
                            } else {
                                next.handle(Future.<Void>succeededFuture());
                                connection.close();
                            }
                        });
                    });
        }
    }

    public void insertOne(Whisky whisky, Handler<AsyncResult<Whisky>> resultHandler) {
        jdbc.getConnection(ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture(ar.cause()));
                return;
            }
            insertOneInsideConnection(whisky, ar.result(), resultHandler);
        });
    }

    private void insertOneInsideConnection(Whisky whisky, SQLConnection connection, Handler<AsyncResult<Whisky>> resultHandler) {
        connection.updateWithParams("INSERT INTO Whisky (name, origin) VALUES ?, ?",
                new JsonArray().add(whisky.getName()).add(whisky.getOrigin()),
                (ar) -> {
                    if (ar.failed()) {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                        return;
                    }
                    UpdateResult result = ar.result();
                    // Build a new whisky instance with the generated id.
                    Whisky w = new Whisky(result.getKeys().getInteger(0), whisky.getName(), whisky.getOrigin());
                    resultHandler.handle(Future.succeededFuture(w));
                });
    }

    public void getAll(Handler<AsyncResult<List<Whisky>>> resultHandler) {
        jdbc.getConnection(ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture(ar.cause()));
                return;
            }
            SQLConnection connection = ar.result();
            connection.query("SELECT * FROM Whisky", result -> {
                if (result.failed()) {
                    resultHandler.handle(Future.failedFuture(result.cause()));
                } else {
                    List<Whisky> whiskies = result.result().getRows().stream()
                            .map(Whisky::new).collect(Collectors.toList());
                    resultHandler.handle(Future.succeededFuture(whiskies));
                }
                connection.close();
            });
        });
    }

    public void getOne(int id, Handler<AsyncResult<Whisky>> resultHandler) {
        jdbc.getConnection(ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture(ar.cause()));
                return;
            }
            SQLConnection connection = ar.result();
            connection.queryWithParams("SELECT * FROM Whisky WHERE ID = ?",
                    new JsonArray().add(id),
                    result -> {
                        if (result.failed()) {
                            resultHandler.handle(Future.failedFuture(result.cause()));
                        } else if (result.result().getNumRows() == 0) {
                            resultHandler.handle(Future.succeededFuture(null));
                        } else {
                            Whisky whisky = new Whisky(result.result().getRows().get(0));
                            resultHandler.handle(Future.succeededFuture(whisky));
                        }
                        connection.close();
                    });
        });
    }

    public void updateOne(Whisky whisky, Handler<AsyncResult<Whisky>> resultHandler) {
        jdbc.getConnection(ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture(ar.cause()));
                return;
            }
            SQLConnection connection = ar.result();
            connection.updateWithParams("UPDATE Whisky SET name = ?, origin = ? WHERE id = ?",
                    new JsonArray().add(whisky.getName()).add(whisky.getOrigin()).add(whisky.getId()),
                    result -> {
                        if (result.failed()) {
                            resultHandler.handle(Future.failedFuture(result.cause()));
                        } else {
                            resultHandler.handle(Future.succeededFuture(whisky));
                        }
                        connection.close();
                    });
        });
    }

    public void deleteOne(int id, Handler<AsyncResult<Void>> resultHandler) {
        jdbc.getConnection(ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture(ar.cause()));
                return;
            }
            SQLConnection connection = ar.result();
            connection.updateWithParams("DELETE FROM Whisky WHERE id = ?",
                    new JsonArray().add(id),
                    result -> {
                        if (result.failed()) {
                            resultHandler.handle(Future.failedFuture(result.cause()));
                        } else {
                            resultHandler.handle(Future.succeededFuture());
                        }
                        connection.close();
                    });
        });
    }
}
