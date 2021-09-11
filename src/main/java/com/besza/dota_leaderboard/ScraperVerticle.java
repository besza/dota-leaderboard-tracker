package com.besza.dota_leaderboard;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

public class ScraperVerticle extends AbstractVerticle {
  private final SqlClient pgClient;
  private final Set<String> players;

  public ScraperVerticle(SqlClient pgClient, Set<String> players) {
    this.pgClient = pgClient;
    this.players = players;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    // TODO: close these resources
    final var webClient = WebClient.create(vertx);

    startPromise.complete();
    go(webClient, pgClient, vertx);
  }

  void go(WebClient webClient, SqlClient pgClient, Vertx vertx) {
    webClient
      .get(80, "www.dota2.com", "/webapi/ILeaderboard/GetDivisionLeaderboard/v0001")
      .addQueryParam("division", "americas")
      .addQueryParam("leaderboard", "0")
      .as(BodyCodec.jsonObject())
      .send()
      .onSuccess(h -> {
        // process the response
        final var nextScheduledPostTime = h.body().getLong("next_scheduled_post_time");
        final var serverTime = h.body().getLong("server_time");
        final var leaderboard = h.body().getJsonArray("leaderboard");
        // TODO: how to properly create an OffsetDateTime of an epoch second?
        final var timePosted = OffsetDateTime.of(
          LocalDateTime.ofEpochSecond(h.body().getLong("time_posted"), 0, ZoneOffset.UTC),
          ZoneOffset.UTC);
        final var rankings = leaderboard
          .stream()
          .map(o -> (JsonObject) o)
          .filter(player -> players.contains(player.getString("name")))
          .peek(System.out::println)
          .map(player -> Tuple.of(timePosted, player))
          .collect(Collectors.toList());

        // persist our newly fetched rankings
        pgClient.preparedQuery("INSERT INTO leaderboard (tstz, rank) VALUES ($1, $2)")
          .executeBatch(rankings, res -> {
            if (res.failed()) {
              System.out.printf("Batch failed: %s%n", res.cause());
            }
          });

        // calculate when to fire next (with a little buffer)
        // TODO: guarantee that the delay is never less than < 1ms, otherwise it won't be scheduled and scraping halts
        final var delayMillis = (nextScheduledPostTime - serverTime + 10L) * 1000L;
        System.out.printf("Next scrape scheduled for %s%n", LocalDateTime.now().plus(delayMillis, ChronoUnit.MILLIS));
        vertx.setTimer(delayMillis, handler -> go(webClient, pgClient, vertx));
      })
      .onFailure(err -> {
        System.out.printf("Something went wrong: %s%n", err.getMessage());
        err.printStackTrace();
      });
  }

}
