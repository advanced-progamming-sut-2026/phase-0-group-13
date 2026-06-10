package model.account;

public class Progress {
    private int passedLevels;
    private int completedMiniGames;
    private int highScore;

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
