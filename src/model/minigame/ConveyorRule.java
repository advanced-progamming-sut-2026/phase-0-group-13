package model.minigame;

import model.game.GameState;
import model.zombie.BaseZombie;

public class ConveyorRule extends MiniGame implements SpecialStageRule {
    public ConveyorRule()
    {}

    @Override
    public void apply(GameState gameState) {

    }

    @Override
    public boolean checkWinCondition() {
        return false;
    }
}
