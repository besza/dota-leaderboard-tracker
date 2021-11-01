package com.besza.dota_leaderboard;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.stream.Collectors;

public class Main2 {
  private static final Logger log = LogManager.getLogger(Main2.class);

  public static void main(String[] args) {
    final var vertx = Vertx.vertx();

    var connectOptions = PgConnectOptions.fromEnv();
    var poolOptions = new PoolOptions().setMaxSize(2);
    var pgPool = PgPool.pool(vertx, connectOptions, poolOptions);

    Handler<AsyncResult<String>> deploymentHandler = h -> {
      if (h.succeeded()) {
        log.info("Verticle deployed successfully with ID {}", h.result());
      } else {
        log.error("Verticle failed to be deployed", h.cause());
      }
    };

    var playerStore = new ConfigStoreOptions()
      .setType("file")
      .setConfig(new JsonObject().put("path", "players.json"));

    var retrieverOptions = new ConfigRetrieverOptions()
      .setScanPeriod(60_000L) // 60 sec
      .addStore(playerStore);

    var configRetriever = ConfigRetriever.create(vertx, retrieverOptions);
    configRetriever.getConfig(json -> {
      // initial setup
      log.info("Loading config file");
      if (json.failed()) {
        log.info("Could not find config file for players");
      } else {
        var config = json.result();
        var players = getPlayersFrom(config);
        vertx.deployVerticle(new ScraperVerticle(pgPool, players), deploymentHandler);
      }
    });

    configRetriever.listen(change -> {
      var previous = change.getPreviousConfiguration();
      var current = change.getNewConfiguration();
      var previousPlayers = getPlayersFrom(previous);
      var currentPlayers = getPlayersFrom(current);
      if (!currentPlayers.isEmpty() && !currentPlayers.equals(previousPlayers)) {
        log.info("Found new configuration list for players. Redeploying..");
        vertx.deploymentIDs().forEach(vertx::undeploy);
        vertx.deployVerticle(new ScraperVerticle(pgPool, currentPlayers), deploymentHandler);
      }
    });
  }

  private static Set<String> getPlayersFrom(JsonObject jsonObject) {
    if (jsonObject.containsKey("players")) {
      return jsonObject.getJsonArray("players")
        .stream()
        .map(e -> (String) e)
        .collect(Collectors.toSet());
    } else {
      return Set.of();
    }
  }
}
