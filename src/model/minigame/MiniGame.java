package model.minigame;

abstract class MiniGame {
    private boolean started;
    private int score;

    public void start() {
        started = true;
    }

    public void end() {
        started = false;
    }

    public abstract boolean checkWinCondition();

    public int getScore() {
        return score;
    }
}
