package model.environment.AttackPatterns;

import java.util.List;
import model.game.GameState;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class StraightAttackPattern implements AttackPattern {
  @Override
  public List<Zombie> findTargets(Plant attacker, GameState gameState) {
    return List.of();
  }
}
