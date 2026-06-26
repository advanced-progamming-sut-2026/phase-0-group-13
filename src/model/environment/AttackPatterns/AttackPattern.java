package model.environment.AttackPatterns;

import java.util.List;
import model.game.GameState;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public interface AttackPattern {
  List<Zombie> findTargets(Plant attacker, GameState gameState);
}
