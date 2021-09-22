package com.besza.dota_leaderboard;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.sqlclient.Pool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ServerVerticle extends AbstractVerticle {

  private final Logger log = LogManager.getLogger(ServerVerticle.class);

  private final Pool pool;

  public ServerVerticle(Pool pool) {
    this.pool = pool;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    var options = new HttpServerOptions().setCompressionSupported(true);
    var server = vertx.createHttpServer(options);
    var router = Router.router(vertx);
    router.get("/leaderboard").respond(ctx -> fetchLeaderboard());
    router.route("/*").handler(StaticHandler.create().setCachingEnabled(false).setIncludeHidden(false));

    server.requestHandler(router).listen(8085)
      .onSuccess(h -> startPromise.complete())
      .onFailure(h -> startPromise.fail("Failed to start HttpServer!"));
  }

  private Future<List<JsonObject>> fetchLeaderboard() {
    return pool.query("SELECT rank, tstz FROM leaderboard ORDER BY tstz DESC")
      .collecting(Collectors.mapping(row -> {
        var rank = row.getJsonObject(0);
        // since we already have a JsonObject, we are just going to throw in the ISO formatted timestamp
        var formatter = DateTimeFormatter.ISO_DATE_TIME;
        var timestamp = row.getOffsetDateTime(1).format(formatter);
        rank.put("recordedAt", timestamp);
        return rank;
      }, Collectors.toList()))
      .execute()
      .onFailure(err -> log.error("Failed to query the database", err))
      .compose(result -> Future.succeededFuture(result.value()));
  }
}
