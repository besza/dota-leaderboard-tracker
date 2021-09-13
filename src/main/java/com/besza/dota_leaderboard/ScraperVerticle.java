package com.besza.dota_leaderboard;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
    // TODO: close these resources
    final var webClient = WebClient.create(vertx);

    startPromise.complete();
    go(webClient, pool, vertx);
  }

  void go(WebClient webClient, Pool pool, Vertx vertx) {
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
        final var timePostedEpoch = h.body().getLong("time_posted");
        // TODO: how to properly create an OffsetDateTime from an epoch second?
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

        // pool operations are not pipelined (sequential)
        pool.withConnection(connection ->
          connection
            .query("SELECT MAX(tstz) FROM leaderboard")
            .execute()
            .compose(rows -> {
              if (rows.iterator().next().getValue(0) != null
                && rows.iterator().next().getOffsetDateTime(0).toEpochSecond() >= timePostedEpoch) {
                log.warn("Refused to save the same batch twice");
                return Future.failedFuture("same batch");
              } else {
                // DB was either empty or we received a new batch
                return connection.preparedQuery("INSERT INTO leaderboard(tstz, rank) VALUES ($1, $2)")
                  .executeBatch(rankings);
              }
            }));

        // calculate when to fire next (with a little buffer)
        // TODO: guarantee that the delay is never less than < 1ms, otherwise it won't be scheduled and scraping halts
        final var delayMillis = (Math.abs(nextScheduledPostTime - serverTime) + 10L) * 1000L;
        log.info("Next scrape scheduled for {}", LocalDateTime.now().plus(delayMillis, ChronoUnit.MILLIS));
        vertx.setTimer(delayMillis, handler -> go(webClient, pool, vertx));
      })
      .onFailure(err -> {
        log.error("Something went wrong: {}", err.getMessage());
        err.printStackTrace();
      });
  }

}
