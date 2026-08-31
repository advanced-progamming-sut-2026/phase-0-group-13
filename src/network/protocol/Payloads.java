package network.protocol;

import com.google.gson.JsonElement;
import java.util.List;
import model.enums.MatchRole;
import model.game.minigame.arcade.IZombieMatch;

public final class Payloads {

  private Payloads() {}

  public record RegisterRequest(
      String username, String password, String nickname, String email, String gender) {}

  public record LoginRequest(String username, String password) {}

  public record TokenLoginRequest(String username, String token) {}

  public record AuthResponse(boolean success, String message, Profile profile, String token) {}

  public record SecurityQuestionRequest(String username, String email) {}

  public record SecurityQuestionResponse(
      boolean success, String message, String questionNumber) {}

  public record PasswordReset(String username, String answer, String newPassword) {}

  public record RenameRequest(String newUsername) {}

  public record Profile(
      String username,
      String nickname,
      String email,
      String gender,
      int coins,
      int diamonds,
      Integer bestScore,
      JsonElement gameData) {}

  public record ProfileUpdate(
      String nickname,
      String email,
      String passwordHash,
      String securityQuestionNumber,
      String securityAnswer,
      int coins,
      int diamonds,
      JsonElement gameData) {}

  public record MatchmakingRequest(String game) {}

  public record MatchInviteRequest(String targetUsername) {}

  public record MatchInviteEvent(String inviteId, String fromUsername) {}

  public record MatchInviteDecision(String inviteId, boolean accepted) {}

  public record MatchFound(String matchId, String opponent, MatchRole role, int level) {}

  public record MatchEnded(
      String matchId, String winner, String loser, MatchRole winningRole, String reason) {}

  public record GameAction(String matchId, String action, String argument, int row, int col) {}

  public record MatchStateUpdate(String matchId, IZombieMatch.Snapshot state) {}

  public enum ReactionKind {
    TEXT,
    EMOJI,
    STICKER
  }

  public record Reaction(String matchId, ReactionKind kind, String value) {}

  public record ReactionEvent(String fromUsername, ReactionKind kind, String value) {}

  public record LeaderboardRequest(int limit) {}

  /**
   * One row of the leaderboard, carrying the columns the doc asks for: the last stage and season
   * the player cleared, how many mini-game levels they have beaten, their completed quests split
   * into daily and non-daily, and their bonus-game record.
   */
  public record LeaderboardEntry(
      String username,
      int lastSeason,
      int lastStage,
      int miniGameLevels,
      int dailyQuests,
      int otherQuests,
      Integer myPoint) {}

  public record LeaderboardResponse(List<LeaderboardEntry> entries) {}

  public record ScoreSubmission(int score) {}

  public record ScoreResponse(boolean improved, Integer bestScore) {}

  public record Ack(boolean success, String message) {}
}
