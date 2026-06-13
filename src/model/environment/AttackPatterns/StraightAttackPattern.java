package model.environment.AttackPatterns;

import model.game.GameState;
import model.plant.BasePlant;
import model.zombie.BaseZombie;

import java.util.List;

public class StraightAttackPattern implements AttackPattern{
    @Override
    public List<BaseZombie> findTargets(BasePlant attacker, GameState gameState) {
        return List.of();
    }
}
