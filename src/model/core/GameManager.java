package model.core;

import model.game.Board;
import model.news.AllNews;

import java.sql.Array;

public class GameManager {
    private Board board;
    private int sunAmount;
    private int plantFoodCount;
    private boolean running;
    private AllNews allnews;

    public GameManager() {
        this.board = null;
        this.sunAmount = 0;
        this.plantFoodCount = 0;
        this.running = false;
        this.allnews = new AllNews(null);
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
