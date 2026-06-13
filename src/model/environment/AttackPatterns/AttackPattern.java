package model.environment.AttackPatterns;

import model.game.GameState;
import model.plant.BasePlant;
import model.zombie.BaseZombie;

import java.util.List;

public interface AttackPattern {
    List<BaseZombie> findTargets(
            BasePlant attacker,
            GameState gameState
    );

}
