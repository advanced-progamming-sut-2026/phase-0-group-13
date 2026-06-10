package model.score;

public class Score {
    private int scoreValue;
    private String username;

    public Score() {
    }

    public Score(String username, int scoreValue) {
        this.username = username;
        this.scoreValue = scoreValue;
    }

    public void setScore(int scoreValue) {
        this.scoreValue = scoreValue;
    }

    public int getScoreValue() {
        return scoreValue;
    }
}
