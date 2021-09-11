package com.besza.dota_leaderboard;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

import java.util.Set;

public class Main {
  public static void main(String[] args) {
    // TODO: should be externally configurable
    final var celebs = Set.of("mason", "Arteezy", "Gunnar");

    final var connectOptions = new PgConnectOptions()
      .setPort(5432)
      .setHost("localhost")
      .setDatabase("postgres")
      .setUser("postgres")
      .setPassword("rooster");

    final var poolOptions = new PoolOptions().setMaxSize(5);
    final var vertx = Vertx.vertx();
    // pooled client operations are pipelined
    final var pgClient = PgPool.client(vertx, connectOptions, poolOptions);

    vertx.deployVerticle(new ScraperVerticle(pgClient, celebs), h -> {
      if (h.succeeded()) {
        System.out.println("Scraper verticle deployed successfully with ID " + h.result());
      } else {
        System.out.println("Something wrong happened " + h.cause());
      }
    });
  }
}
