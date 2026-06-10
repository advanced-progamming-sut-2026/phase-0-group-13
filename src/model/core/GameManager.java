package model.core;

import model.game.Board;

public class GameManager {
    private Board board;
    private int sunAmount;
    private int plantFoodCount;
    private boolean running;

    public GameManager() {
        this.board = null;
        this.sunAmount = 0;
        this.plantFoodCount = 0;
        this.running = false;
    }

    public GameManager(Board board) {
        this.board = board;
        this.sunAmount = 0;
        this.plantFoodCount = 0;
        this.running = false;
    }

    public void startGame() {
        running = true;
    }

    public void advanceTime() {
    }

    public void endGame() {
        running = false;
    }

    public int getSunAmount() {
        return sunAmount;
    }

    public int getPlantFoodCount() {
        return plantFoodCount;
    }
}
