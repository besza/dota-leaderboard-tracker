package com.besza.dota_leaderboard;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

import java.util.HashSet;
import java.util.Set;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    WebClient client = WebClient.create(vertx);

    Set<String> celebs = new HashSet<>();
    celebs.add("mason");
    celebs.add("Arteezy");
    celebs.add("Gunnar");

    client
      .get(80, "www.dota2.com", "/webapi/ILeaderboard/GetDivisionLeaderboard/v0001")
      .addQueryParam("division", "americas")
      .addQueryParam("leaderboard", "0")
      .as(BodyCodec.json(LeaderboardResponse.class))
      .send(ar -> {
        if (ar.succeeded()) {
          ar.result().body().getLeaderboard()
            .stream()
            .filter(player -> celebs.contains(player.getName()))
            .forEach(System.out::println);
        } else {
          System.out.println("Something went wrong.." + ar.cause().getMessage());
          ar.cause().printStackTrace();
        }
      });

    // ??
    client.close();
    this.stop();
  }
}
