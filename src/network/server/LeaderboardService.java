package network.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import network.protocol.Payloads;

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
