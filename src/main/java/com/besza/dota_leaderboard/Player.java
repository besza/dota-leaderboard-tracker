package com.besza.dota_leaderboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Player {
  private int rank;
  private String name;

  public Player() {
  }

  public int getRank() {
    return rank;
  }

  public void setRank(int rank) {
    this.rank = rank;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "Player{" +
      "rank=" + rank +
      ", name='" + name + '\'' +
      '}';
  }
}
