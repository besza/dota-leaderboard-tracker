package com.besza.dota_leaderboard;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;

public class ScraperVerticle extends AbstractVerticle {

  private final Logger log = LogManager.getLogger(ScraperVerticle.class);

  private final Pool pool;
  private final Set<String> players;

  public ScraperVerticle(Pool pool, Set<String> players) {
    this.pool = pool;
    this.players = players;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    var webClient = WebClient.create(vertx);
    fetch(webClient);
    vertx.setPeriodic(Duration.ofHours(1L).toMillis(), h -> fetch(webClient));
    startPromise.complete();
  }

  void fetch(WebClient webClient) {
    webClient
      .get("www.dota2.com", "/webapi/ILeaderboard/GetDivisionLeaderboard/v0001")
      .addQueryParam("division", "americas")
      .addQueryParam("leaderboard", "0")
      .as(BodyCodec.jsonObject())
      .send()
      .onSuccess(h -> {
        // process the response
        final var leaderboard = h.body().getJsonArray("leaderboard");
        final var timePostedEpoch = h.body().getLong("time_posted");
        final var timePosted = OffsetDateTime.of(
          LocalDateTime.ofEpochSecond(timePostedEpoch, 0, ZoneOffset.UTC),
          ZoneOffset.UTC);
        final var rankings = leaderboard
          .stream()
          .map(o -> (JsonObject) o)
          .filter(player -> players.contains(player.getString("name")))
          .peek(log::info)
          .map(player -> Tuple.of(timePosted, player))
          .collect(Collectors.toList());

        pool.preparedQuery("INSERT INTO leaderboard(tstz, rank) VALUES ($1, $2) ON CONFLICT DO NOTHING")
          .executeBatch(rankings)
          .onFailure(err -> log.error("Failed to execute database query", err));
      })
      .onFailure(err -> log.error("Something went wrong: {}", err.getMessage()));
  }

}
