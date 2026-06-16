package model.environment.AttackPatterns;

import model.game.GameState;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

import java.util.List;

public class StraightAttackPattern implements AttackPattern{
    @Override
    public List<Zombie> findTargets(Plant attacker, GameState gameState) {
        return List.of();
    }
}
