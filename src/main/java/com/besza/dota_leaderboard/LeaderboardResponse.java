package com.besza.dota_leaderboard;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class LeaderboardResponse {
  @JsonProperty("time_posted")
  private Long timePosted;
  @JsonProperty("next_scheduled_post_time")
  private Long nextScheduledPostTime;
  @JsonProperty("server_time")
  private Long serverTime;
  private List<Player> leaderboard;

  public LeaderboardResponse() {
  }

  public Long getTimePosted() {
    return timePosted;
  }

  public void setTimePosted(Long timePosted) {
    this.timePosted = timePosted;
  }

  public Long getNextScheduledPostTime() {
    return nextScheduledPostTime;
  }

  public void setNextScheduledPostTime(Long nextScheduledPostTime) {
    this.nextScheduledPostTime = nextScheduledPostTime;
  }

  public Long getServerTime() {
    return serverTime;
  }

  public void setServerTime(Long serverTime) {
    this.serverTime = serverTime;
  }

  public List<Player> getLeaderboard() {
    return leaderboard;
  }

  public void setLeaderboard(List<Player> leaderboard) {
    this.leaderboard = leaderboard;
  }
}
