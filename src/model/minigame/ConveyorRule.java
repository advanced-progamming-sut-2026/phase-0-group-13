package model.minigame;

import model.game.GameState;

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
