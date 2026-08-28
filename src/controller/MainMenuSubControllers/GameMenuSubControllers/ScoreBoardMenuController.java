package controller.MainMenuSubControllers.GameMenuSubControllers;

import controller.BaseController;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import model.core.App;
import model.enums.Commands.MenuCommands;
import model.enums.Commands.ScoreBoardMenuCommands;
import model.enums.Menu;
import network.client.ClientSession;
import network.protocol.Payloads;

/**
 * The terminal leaderboard.
 *
 * <p>Rows come from the server, the same LEADERBOARD_REQUEST the graphical LeaderboardScreen
 * sends, so both builds rank the same players off the same authoritative data. It used to read
 * UserManager.getAllUsers(), which is only the accounts this machine happens to have cached
 * locally -- on a fresh install that is one row, the player themselves.
 */
public class ScoreBoardMenuController implements BaseController {

  /** 0 asks for every row; the terminal has no paging, so it prints them all. */
  private static final int NO_LIMIT = 0;

  private String lastSortedLabel = null;
  private boolean lastAscending = false;

  @Override
  public void initController() {}

  @Override
  public void handleinput(String command) {
    if (ScoreBoardMenuCommands.SortByHighScore.getMatcher(command) != null) {
      handleShow("High Score (MyoPoints)",
          Comparator.comparingInt(ScoreBoardMenuController::meowPoints));
    } else if (ScoreBoardMenuCommands.SortByLastPassedLevel.getMatcher(command) != null) {
      handleShow("Last Passed Level", Comparator
          .comparingInt(Payloads.LeaderboardEntry::lastSeason)
          .thenComparingInt(Payloads.LeaderboardEntry::lastStage));
    } else if (ScoreBoardMenuCommands.SortByMiniGames.getMatcher(command) != null) {
      handleShow("Mini-Games Cleared",
          Comparator.comparingInt(Payloads.LeaderboardEntry::miniGameLevels));
    } else if (ScoreBoardMenuCommands.SortByQuests.getMatcher(command) != null) {
      handleShow("Quests Completed",
          Comparator.comparingInt(ScoreBoardMenuController::completedQuests));
    } else if (ScoreBoardMenuCommands.SortByDailyQuests.getMatcher(command) != null) {
      handleShow("Daily Quests Completed",
          Comparator.comparingInt(Payloads.LeaderboardEntry::dailyQuests));
    } else if (MenuCommands.ShowCurrentMenu.getMatcher(command) != null) {
      System.out.println("Leaderboard Menu (Score Board)");
    } else if (MenuCommands.ExitMenu.getMatcher(command) != null) {
      exit();
    } else {
      System.out.println("invalid input");
    }
  }

  private boolean resolveDirection(String label) {
    boolean ascending = label.equals(lastSortedLabel) && !lastAscending;
    lastSortedLabel = label;
    lastAscending = ascending;
    return ascending;
  }

  private void handleShow(String label,
      Comparator<Payloads.LeaderboardEntry> ascendingComparator) {
    List<Payloads.LeaderboardEntry> ranked;
    try {
      ranked = fetch();
    } catch (IOException e) {
      System.out.println("Could not reach the server: " + e.getMessage());
      return;
    }
    if (ranked == null) {
      return;
    }
    if (ranked.isEmpty()) {
      System.out.println("No registered users yet.");
      return;
    }

    boolean ascending = resolveDirection(label);
    Comparator<Payloads.LeaderboardEntry> directionalComparator =
            ascending ? ascendingComparator : ascendingComparator.reversed();
    ranked.sort(directionalComparator.thenComparing(Payloads.LeaderboardEntry::username));

    printLeaderboard(label, ascending, ranked);
  }

  /**
   * The server's rows, or null when it could not be asked -- the reason is printed by then.
   *
   * <p>The server answers this only for a signed-in connection, and an unauthenticated request
   * comes back as an ERROR rather than an empty table, so the two cases are told apart here
   * instead of both looking like "nobody has registered".
   */
  private static List<Payloads.LeaderboardEntry> fetch() throws IOException {
    ClientSession session = ClientSession.getInstance();
    if (!session.isConnected() && !session.connect()) {
      System.out.println("Could not reach the server: " + session.getLastError());
      return null;
    }
    if (!session.isAuthenticated()) {
      System.out.println("You must be logged in to see the leaderboard.");
      return null;
    }
    return new ArrayList<>(session.requestLeaderboard(NO_LIMIT).entries());
  }

  private void printLeaderboard(String label, boolean ascending,
      List<Payloads.LeaderboardEntry> ranked) {
    System.out.println(
            "\n--- Global Leaderboard (Sorted by: "
                    + label
                    + ", "
                    + (ascending ? "Ascending" : "Descending")
                    + ") ---");
    System.out.printf(
            "%-5s | %-15s | %-10s | %-10s | %-10s | %-15s%n",
            "Rank", "Username", "MyoPoints", "Stages", "MiniGames", "Quests(Tot/Day)");
    System.out.println("-".repeat(78));

    int rank = 1;
    for (Payloads.LeaderboardEntry entry : ranked) {
      System.out.printf(
              "%-5d | %-15s | %-10s | %-10s | %-10d | %d / %d%n",
              rank,
              entry.username(),
              // Blank, not zero: a player who never finished a bonus run has no MyoPoint.
              entry.myPoint() == null ? "-" : String.valueOf(entry.myPoint()),
              progressOf(entry),
              entry.miniGameLevels(),
              completedQuests(entry),
              entry.dailyQuests());
      rank++;
    }
    System.out.println();
  }

  /** "2-3" for season 2 stage 3, or a dash for an account that has cleared nothing yet. */
  static String progressOf(Payloads.LeaderboardEntry entry) {
    if (entry.lastSeason() <= 0 || entry.lastStage() <= 0) {
      return "-";
    }
    return entry.lastSeason() + "-" + entry.lastStage();
  }

  /** Ranks an unplayed bonus game below a zero-scoring one rather than alongside it. */
  static int meowPoints(Payloads.LeaderboardEntry entry) {
    return entry.myPoint() == null ? Integer.MIN_VALUE : entry.myPoint();
  }

  static int completedQuests(Payloads.LeaderboardEntry entry) {
    return entry.dailyQuests() + entry.otherQuests();
  }

  @Override
  public void exit() {
    System.out.println("Changed to Game Menu.");
    App.setCurrentMenu(Menu.GameMenu);
  }
}
