package model.game.plant.behavior;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class HypnotiseAction implements PlantAction {

  private final int actionInterval;
  private final int targets;

  public HypnotiseAction(int actionInterval, int targets) {
    this.actionInterval = Math.max(1, actionInterval);
    this.targets = Math.max(1, targets);
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (currentTick - plant.getLastActionTick() < actionInterval) {
      return;
    }

    List<Zombie> candidates = new ArrayList<>();
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead() && !zombie.isHypnotized()) {
        candidates.add(zombie);
      }
    }
    if (candidates.isEmpty()) {
      return;
    }

    Collections.shuffle(candidates);
    int hypnotised = Math.min(targets, candidates.size());
    for (int i = 0; i < hypnotised; i++) {
      Zombie zombie = candidates.get(i);
      zombie.setHypnotized(true);
      zombie.setEating(false);
      System.out.printf("%s hypnotised %s; it now fights for you!%n",
              plant.getName(), zombie.getName());
    }
    plant.setLastActionTick(currentTick);
  }
}
