package model.environment.AttackPatterns;

import model.game.GameState;
import model.plant.Plant;
import model.zombie.Zombie;

import java.util.List;

public interface AttackPattern {
    List<Zombie> findTargets(
            Plant attacker,
            GameState gameState
    );

}
