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

  /** Used for both register and login. profile is null when it failed; token only on login. */
  public record AuthResponse(boolean success, String message, Profile profile, String token) {}

  public record SecurityQuestionRequest(String username, String email) {}

  public record SecurityQuestionResponse(
      boolean success, String message, String questionNumber) {}

  /** A null newPassword only checks the answer, so the menus can ask in two steps. */
  public record PasswordReset(String username, String answer, String newPassword) {}

  public record RenameRequest(String newUsername) {}

  /**
   * The account as the server holds it.
   *
   * <p>gameData is the player's own document - the same shape data/database/Users.json used to
   * hold - carried as raw JSON so the server does not have to know the game model to store it and
   * nothing has to be re-listed here every time the model grows a field. Null for an account the
   * server has never been given data for.
   */
  public record Profile(
      String username,
      String nickname,
      String email,
      String gender,
      int coins,
      int diamonds,
      Integer bestScore,
      JsonElement gameData) {}

  /**
   * Writes the signed-in account's data back to the server.
   *
   * <p>The username is not in here on purpose: it is the server's key for the account, taken from
   * the authenticated connection. bestScore is not in here either - that record belongs to
   * LeaderboardService and only SCORE_SUBMISSION may move it.
   */
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

  /** level and seed so a client can name the match it is in; the board itself comes in state. */
  public record MatchFound(String matchId, String opponent, MatchRole role, int level) {}

  /** winner and loser are usernames; winningRole is which side of the board took it. */
  public record MatchEnded(
      String matchId, String winner, String loser, MatchRole winningRole, String reason) {}

  public record GameAction(String matchId, String action, String argument, int row, int col) {}

  /**
   * The whole authoritative board, once per server tick.
   *
   * <p>It used to carry a single formatted string, which meant a client could show a brain count
   * and nothing else and had to simulate the rest to draw it. Carrying the engine's own snapshot
   * is what makes the server the only place the game actually runs.
   */
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
   *
   * <p>Everything but myPoint is read out of the account's stored game data, so the table is about
   * the whole game rather than about the bonus game alone. myPoint is the account's bestScore and
   * is null - not zero - for a player who has never submitted one, which is what keeps the column
   * empty for them instead of ranking them last with a score they never played for.
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
