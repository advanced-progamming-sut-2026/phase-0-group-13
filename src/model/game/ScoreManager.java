package model.game;

import model.account.User;
import model.enums.ScoreEvent;

public class ScoreManager {
  private int currentMatchScore;

  public ScoreManager() {
    this.currentMatchScore = 0;
  }

  public void triggerEvent(ScoreEvent event, int multiplier) {
    int earned = event.getPoints() * multiplier;
    currentMatchScore += earned;
    System.out.println("[BONUS] +" + earned + " MyoPoints for: " + event.name());
  }

  public void triggerEvent(ScoreEvent event) {
    triggerEvent(event, 1);
  }

  public int getCurrentMatchScore() {
    return currentMatchScore;
  }

  public void applyScoresToUser(User user) {
    if (user != null && currentMatchScore > 0) {
      user.addMeowPoints(currentMatchScore);
      System.out.println("Total MyoPoints gained this match: " + currentMatchScore);
      currentMatchScore = 0;
    }
  }
}
