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

  private final List<Reward> pendingRewards = new ArrayList<>();
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
    System.out.printf(
            "A zombie dropped a %s; you have %d %s now.%n", dropName, runningTotal, unit);
  }

  public List<Reward> drainPendingRewards() {
    List<Reward> drained = new ArrayList<>(pendingRewards);
    pendingRewards.clear();
    return drained;
  }
}
