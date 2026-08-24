package network.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import network.protocol.Payloads;

/**
 * The leaderboard, built from what the server holds rather than from anything a client says.
 *
 * <p>The doc wants one row per registered player carrying the last stage and season they cleared,
 * how many mini-game levels they have beaten, their completed quests split into daily and
 * non-daily, and their bonus-game record -- and it wants the table to describe the whole game, not
 * just the bonus game. All of that except the record is already on the server inside the account's
 * {@code gameData} document, so the rows are read straight out of it. That is also what makes the
 * table update by itself: every PROFILE_UPDATE rewrites the document the next request reads.
 *
 * <p>gameData is deliberately opaque to the server everywhere else -- it is stored and returned
 * verbatim so the protocol does not have to grow a field every time the game model does. Reading
 * it here is the one exception, and it is done defensively: a missing or oddly-shaped field
 * becomes a zero for that column rather than a failed request.
 */
public final class LeaderboardService {

  private final ServerAccountStore store;

  public LeaderboardService(ServerAccountStore store) {
    this.store = store;
  }

  /** Keeps the higher of the two, so a worse run never overwrites a record. */
  public Payloads.ScoreResponse submit(String username, int score) {
    ServerAccount account = store.find(username);
    if (account == null) {
      return new Payloads.ScoreResponse(false, null);
    }
    Integer best = account.getBestScore();
    if (best != null && best >= score) {
      return new Payloads.ScoreResponse(false, best);
    }
    account.setBestScore(score);
    store.save();
    return new Payloads.ScoreResponse(true, score);
  }

  /**
   * Every registered player, best bonus score first.
   *
   * <p>Unlike the bonus-game record, a row is not conditional on having played: the doc says the
   * table lists "all players who have registered". Players with no record sort last and carry a
   * null My Point so the column shows blank for them.
   */
  public Payloads.LeaderboardResponse top(int limit) {
    List<Payloads.LeaderboardEntry> entries = new ArrayList<>();
    for (ServerAccount account : store.all()) {
      entries.add(rowFor(account));
    }
    entries.sort(
        Comparator.comparing(
                (Payloads.LeaderboardEntry e) -> e.myPoint() == null ? Integer.MIN_VALUE
                    : e.myPoint())
            .reversed()
            .thenComparing(Payloads.LeaderboardEntry::username));
    int size = limit > 0 ? Math.min(limit, entries.size()) : entries.size();
    return new Payloads.LeaderboardResponse(new ArrayList<>(entries.subList(0, size)));
  }

  private Payloads.LeaderboardEntry rowFor(ServerAccount account) {
    JsonObject data = asObject(account.getGameData());
    JsonObject progress = data == null ? null : asObject(data.get("progress"));

    int season = intAt(progress, "maxClearedStage");
    int stage = intAt(progress, "maxClearedLevel");
    int miniGameLevels = 0;
    JsonObject cleared = progress == null ? null : asObject(progress.get("clearedMiniGameLevels"));
    if (cleared != null) {
      for (Map.Entry<String, JsonElement> entry : cleared.entrySet()) {
        miniGameLevels += asInt(entry.getValue());
      }
    }

    int daily = 0;
    int other = 0;
    JsonArray quests = data != null && data.get("quests") != null && data.get("quests").isJsonArray()
        ? data.getAsJsonArray("quests")
        : null;
    if (quests != null) {
      for (JsonElement element : quests) {
        JsonObject quest = asObject(element);
        if (quest == null || !isTrue(quest.get("isCompleted"))) {
          continue;
        }
        String category = stringAt(quest, "Category");
        if (category.toLowerCase().contains("daily")) {
          daily++;
        } else {
          other++;
        }
      }
    }

    return new Payloads.LeaderboardEntry(account.getUsername(), season, stage, miniGameLevels,
        daily, other, account.getBestScore());
  }

  private static JsonObject asObject(JsonElement element) {
    return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
  }

  private static int intAt(JsonObject owner, String field) {
    return owner == null ? 0 : asInt(owner.get(field));
  }

  private static int asInt(JsonElement element) {
    try {
      return element != null && element.isJsonPrimitive() ? element.getAsInt() : 0;
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static String stringAt(JsonObject owner, String field) {
    JsonElement element = owner == null ? null : owner.get(field);
    return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
  }

  private static boolean isTrue(JsonElement element) {
    try {
      return element != null && element.isJsonPrimitive() && element.getAsBoolean();
    } catch (RuntimeException e) {
      return false;
    }
  }
}
