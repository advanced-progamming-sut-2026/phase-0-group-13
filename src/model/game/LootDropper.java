package model.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.game.reward.Currency;
import model.game.reward.Reward;
import model.game.zombie.Zombie;

public class LootDropper {

  private static final double DEATH_DROP_CHANCE = 0.10;
  private static final int COIN_DROP_AMOUNT = 50;
  private static final int MAX_PENDING_NOTICES = 8;
  private static final int MAX_PENDING_LOOT_SPAWNS = 24;

  /** Where a drop happened, so the graphical build can show something on the lawn there. */
  public record LootSpawn(String kind, double column, int row) {}

  private final List<Reward> pendingRewards = new ArrayList<>();
  // The same lines the terminal prints, so the graphical build can show them too.
  private final List<String> pendingNotices = new ArrayList<>();
  // Terminal mode never drains this; the cap keeps a long match from growing it forever.
  private final List<LootSpawn> pendingLootSpawns = new ArrayList<>();
  private final Random random;

  private int droppedCoins;
  private int droppedDiamonds;
  private int droppedPots;

  public LootDropper(Random random) {
    this.random = random;
  }

  public void rollFor(Zombie zombie) {
    if (random.nextDouble() >= DEATH_DROP_CHANCE) {
      return;
    }

    int roll = random.nextInt(3);
    Reward reward;
    String dropName;
    int runningTotal;
    String unit;

    if (roll == 0) {
      reward = new Currency("COIN", COIN_DROP_AMOUNT);
      dropName = "coin";
      droppedCoins += COIN_DROP_AMOUNT;
      runningTotal = droppedCoins;
      unit = "coins";
    } else if (roll == 1) {
      reward = new Currency("DIAMOND", 1);
      dropName = "diamond";
      droppedDiamonds += 1;
      runningTotal = droppedDiamonds;
      unit = "diamonds";
    } else {
      reward = new model.game.reward.Inventory("pot", 1);
      dropName = "pot";
      droppedPots += 1;
      runningTotal = droppedPots;
      unit = "pots";
    }

    pendingRewards.add(reward);
    String notice = String.format(
            "A zombie dropped a %s; you have %d %s now.", dropName, runningTotal, unit);
    // The terminal never drains, so the queue is capped rather than left to grow all match.
    if (pendingNotices.size() >= MAX_PENDING_NOTICES) {
      pendingNotices.remove(0);
    }
    pendingNotices.add(notice);
    System.out.println(notice);

    if (pendingLootSpawns.size() >= MAX_PENDING_LOOT_SPAWNS) {
      pendingLootSpawns.remove(0);
    }
    pendingLootSpawns.add(new LootSpawn(dropName, zombie.getX(), zombie.getRow()));
  }

  public List<Reward> drainPendingRewards() {
    List<Reward> drained = new ArrayList<>(pendingRewards);
    pendingRewards.clear();
    return drained;
  }

  public List<String> drainPendingNotices() {
    List<String> drained = new ArrayList<>(pendingNotices);
    pendingNotices.clear();
    return drained;
  }

  public List<LootSpawn> drainPendingLootSpawns() {
    List<LootSpawn> drained = new ArrayList<>(pendingLootSpawns);
    pendingLootSpawns.clear();
    return drained;
  }
}
