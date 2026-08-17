package network.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

  /** Only accounts that actually submitted a score appear. */
  public Payloads.LeaderboardResponse top(int limit) {
    List<Payloads.LeaderboardEntry> entries = new ArrayList<>();
    for (ServerAccount account : store.all()) {
      if (account.getBestScore() != null) {
        entries.add(new Payloads.LeaderboardEntry(account.getUsername(), account.getBestScore()));
      }
    }
    entries.sort(Comparator.comparingInt(Payloads.LeaderboardEntry::score).reversed()
        .thenComparing(Payloads.LeaderboardEntry::username));
    int size = limit > 0 ? Math.min(limit, entries.size()) : entries.size();
    return new Payloads.LeaderboardResponse(new ArrayList<>(entries.subList(0, size)));
  }
}
