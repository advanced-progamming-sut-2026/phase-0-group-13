package model.game.plant.behavior;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.game.Board;
import model.game.Projectile;
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
      castAt(plant, board, zombie);
      System.out.printf("%s cast its charm at %s.%n", plant.getName(), zombie.getName());
    }
    plant.setLastActionTick(currentTick);
  }

  /**
   * Sends a magic shot at the target instead of turning it where it stands.
   *
   * <p>The doc says Caulipower "fires a magic shot ... hypnotises its target", and it was doing
   * the second half without the first: the zombie simply changed sides with nothing on screen to
   * say why. The charm rides the shot, so what the player sees and what the model does are the
   * same event. It passes obstacles the way the doc asks by piercing, and carries no damage.
   */
  private void castAt(Plant plant, Board board, Zombie target) {
    Projectile charm = new Projectile(0, 0.5, plant.getCol(), target.getRow(),
            Projectile.ProjectileEffect.NORMAL, true, false, false);
    charm.firedBy(plant.getName());
    charm.withPierceLimit(1);
    charm.thatHypnotises();
    charm.setDirection(target.getX() >= plant.getCol() ? 1 : -1, 0);
    board.addProjectile(charm);
  }
}
