package model.game;

import java.util.ArrayList;
import java.util.List;
import model.account.User;
import model.enums.ScoreEvent;

public class ScoreManager {
  private static final int MAX_PENDING_NOTICES = 6;

  private int currentMatchScore;
  // The same lines the terminal prints, kept so the graphical build can show them too.
  private final List<String> pendingNotices = new ArrayList<>();

  public ScoreManager() {
    this.currentMatchScore = 0;
  }

  public void triggerEvent(ScoreEvent event, int multiplier) {
    int earned = event.getPoints() * multiplier;
    currentMatchScore += earned;
    System.out.println("[BONUS] +" + earned + " MyoPoints for: " + event.name());
    queue(describe(event) + "   +" + earned + " MyoPoints");
  }

  public void triggerEvent(ScoreEvent event) {
    triggerEvent(event, 1);
  }

  /**
   * What the player is told when a scoring pattern fires.
   *
   * <p>The doc asks for a notification whenever one of these patterns happens, and the enum's own
   * name ({@code MULTI_KILL_ONE_SHOT}) is not something to put in front of a player.
   */
  private static String describe(ScoreEvent event) {
    return switch (event) {
      case MULTI_KILL_ONE_SHOT -> "Multi-kill! One shot, several zombies.";
      case KILL_FAST_ZOMBIE -> "Caught a runner!";
      case SIMULTANEOUS_KILL -> "Double kill!";
      case WAVE_CLEARED_NO_LOSS -> "Flawless wave -- not a plant lost!";
      case SPEED_SUN_COLLECT -> "Quick hands! Sun caught on the drop.";
    };
  }

  private void queue(String notice) {
    // Nothing drains this in terminal mode, so the queue is capped rather than left to grow.
    if (pendingNotices.size() >= MAX_PENDING_NOTICES) {
      pendingNotices.remove(0);
    }
    pendingNotices.add(notice);
  }

  /** Pattern notifications since the last call, for the HUD to show. */
  public List<String> drainPendingNotices() {
    List<String> drained = new ArrayList<>(pendingNotices);
    pendingNotices.clear();
    return drained;
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
