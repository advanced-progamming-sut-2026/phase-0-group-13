package model.account;

import model.game.Wave;

import java.util.Set;

public class Progress {
    private int passedLevels;
    private int completedMiniGames;
    private int highScore;
    private Set<String> completedStages;
    private Set<String> unlockedStages;
    private AdventureMap adventureMap;
    private Wave wave;

    public Progress() {
    }

    public Progress(int passedLevels, int completedMiniGames, int highScore) {
        this.passedLevels = passedLevels;
        this.completedMiniGames = completedMiniGames;
        this.highScore = highScore;
    }

    public void passLevel() {
        passedLevels++;
    }

    public void completeMiniGame() {
        completedMiniGames++;
    }

    public void updateHighScore() {
    }
}
