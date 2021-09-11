package com.besza.dota_leaderboard;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.SqlClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class ServerVerticle extends AbstractVerticle {

  private final Logger log = LogManager.getLogger();

  private final SqlClient pgClient;

  public ServerVerticle(SqlClient pgClient) {
    this.pgClient = pgClient;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    var server = vertx.createHttpServer();
    var router = Router.router(vertx);
    router.get("/leaderboard").respond(ctx -> fetchLeaderboard());

    server.requestHandler(router).listen(8080)
      .onSuccess(h -> startPromise.complete())
      .onFailure(h -> startPromise.fail("Failed to start HttpServer!"));
  }

  private Future<List<JsonObject>> fetchLeaderboard() {
    return pgClient.query("SELECT rank FROM leaderboard ORDER BY tstz")
      .collecting(Collectors.mapping(row -> row.getJsonObject(0), Collectors.toList()))
      .execute()
      .onFailure(err -> log.error("Failed to query the database", err))
      .compose(result -> Future.succeededFuture(result.value()));
  }
}
