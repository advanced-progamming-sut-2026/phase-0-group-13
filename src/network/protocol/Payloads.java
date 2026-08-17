package network.protocol;

import java.util.List;
import model.enums.MatchRole;

public final class Payloads {

  private Payloads() {}

  public record RegisterRequest(
      String username, String password, String nickname, String email, String gender) {}

  public record LoginRequest(String username, String password) {}

  /** Used for both register and login. profile is null when it failed. */
  public record AuthResponse(boolean success, String message, Profile profile) {}

  public record Profile(
      String username, String nickname, int coins, int diamonds, Integer bestScore) {}

  public record MatchmakingRequest(String game) {}

  public record MatchInviteRequest(String targetUsername) {}

  public record MatchInviteEvent(String inviteId, String fromUsername) {}

  public record MatchInviteDecision(String inviteId, boolean accepted) {}

  public record MatchFound(String matchId, String opponent, MatchRole role) {}

  public record MatchEnded(String matchId, String winner, String reason) {}

  public record GameAction(String matchId, String action, String argument, int row, int col) {}

  public record MatchStateUpdate(String matchId, String state) {}

  public enum ReactionKind {
    TEXT,
    EMOJI,
    STICKER
  }

  public record Reaction(String matchId, ReactionKind kind, String value) {}

  public record ReactionEvent(String fromUsername, ReactionKind kind, String value) {}

  public record LeaderboardRequest(int limit) {}

  public record LeaderboardEntry(String username, int score) {}

  public record LeaderboardResponse(List<LeaderboardEntry> entries) {}

  public record ScoreSubmission(int score) {}

  public record ScoreResponse(boolean improved, Integer bestScore) {}

  public record Ack(boolean success, String message) {}
}
