package controller.MainMenuSubControllers.GameMenuSubControllers;

import controller.BaseController;
import data.persistence.UserManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import model.account.User;
import model.core.App;
import model.enums.Commands.MenuCommands;
import model.enums.Commands.ScoreBoardMenuCommands;
import model.enums.Menu;
import model.game.quest.Quest;

public class ScoreBoardMenuController implements BaseController {

  private String lastSortedLabel = null;
  private boolean lastAscending = false;

  @Override
  public void initController() {}

  @Override
  public void handleinput(String command) {
    if (ScoreBoardMenuCommands.SortByHighScore.getMatcher(command) != null) {
      handleShow("High Score (MyoPoints)", Comparator.comparingInt(this::meowPoints));
    } else if (ScoreBoardMenuCommands.SortByLastPassedLevel.getMatcher(command) != null) {
      handleShow("Last Passed Level", Comparator.comparingInt(this::stageLevelScore));
    } else if (ScoreBoardMenuCommands.SortByMiniGames.getMatcher(command) != null) {
      handleShow("Mini-Games Cleared", Comparator.comparingInt(this::miniGamesCleared));
    } else if (ScoreBoardMenuCommands.SortByQuests.getMatcher(command) != null) {
      handleShow("Quests Completed", Comparator.comparingInt(this::completedQuests));
    } else if (ScoreBoardMenuCommands.SortByDailyQuests.getMatcher(command) != null) {
      handleShow("Daily Quests Completed", Comparator.comparingInt(this::completedDailyQuests));
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

  private void handleShow(String label, Comparator<User> ascendingComparator) {
    List<User> ranked = new ArrayList<>(UserManager.getInstance().getAllUsers());
    if (ranked.isEmpty()) {
      System.out.println("No registered users yet.");
      return;
    }

    boolean ascending = resolveDirection(label);
    Comparator<User> directionalComparator =
            ascending ? ascendingComparator : ascendingComparator.reversed();
    Comparator<User> finalComparator = directionalComparator.thenComparing(User::getUsername);
    ranked.sort(finalComparator);

    printLeaderboard(label, ascending, ranked);
  }

  private void printLeaderboard(String label, boolean ascending, List<User> ranked) {
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
    for (User user : ranked) {
      System.out.printf(
              "%-5d | %-15s | %-10d | %-10d | %-10d | %d / %d%n",
              rank,
              user.getUsername(),
              meowPoints(user),
              stageLevelScore(user),
              miniGamesCleared(user),
              completedQuests(user),
              completedDailyQuests(user));
      rank++;
    }
    System.out.println();
  }

  private int meowPoints(User user) {
    return user.getMeowPoints();
  }

  private int stageLevelScore(User user) {
    int count = 0;
    for (String stage : user.getUnlockedStages()) {
      if (stage.startsWith("stage_")) count++;
    }
    return count;
  }

  private int miniGamesCleared(User user) {
    int count = 0;
    for (String stage : user.getUnlockedStages()) {
      if (stage.startsWith("minigame_")) count++;
    }
    return count;
  }

  private int completedQuests(User user) {
    int count = 0;
    for (Quest quest : user.getQuests()) {
      if (quest.isCompleted()) count++;
    }
    return count;
  }

  private int completedDailyQuests(User user) {
    int count = 0;
    for (Quest quest : user.getQuests()) {
      if (quest.isCompleted()
              && quest.getCategory() != null
              && (quest.getCategory().toLowerCase().contains("daily")
              || quest.getCategory().contains("روزانه"))) {
        count++;
      }
    }
    return count;
  }

  @Override
  public void exit() {
    System.out.println("Changed to Game Menu.");
    App.setCurrentMenu(Menu.GameMenu);
  }
}