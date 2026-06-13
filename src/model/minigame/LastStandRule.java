package model.minigame;

import model.game.GameState;

public class LastStandRule extends MiniGame implements SpecialStageRule{
    public LastStandRule()
    {}
    @Override
    public boolean checkWinCondition() {
        return false;
    }

    @Override
    public void apply(GameState gameState) {

    }
}
