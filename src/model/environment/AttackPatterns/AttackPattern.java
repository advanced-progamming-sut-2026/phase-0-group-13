package model.environment.AttackPatterns;

import model.game.GameState;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

import java.util.List;

public interface AttackPattern {
    List<Zombie> findTargets(
            Plant attacker,
            GameState gameState
    );

}
