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

  @Override
  public void initController() {}

  @Override
  public void handleinput(String command) {
    if (ScoreBoardMenuCommands.SortByHighScore.getMatcher(command) != null) {
      handleShow("High Score", Comparator.comparingInt(this::meowPoints).reversed(), this::meowPoints);
    } else if (ScoreBoardMenuCommands.SortByLastPassedLevel.getMatcher(command) != null) {
      handleShow(
              "Last Passed Level",
              Comparator.comparingInt(this::stageLevelScore).reversed(),
              this::stageLevelScore);
    } else if (ScoreBoardMenuCommands.SortByMiniGames.getMatcher(command) != null) {
      handleShow(
              "Mini-Games Cleared",
              Comparator.comparingInt(this::miniGamesCleared).reversed(),
              this::miniGamesCleared);
    } else if (ScoreBoardMenuCommands.SortByQuests.getMatcher(command) != null) {
      handleShow(
              "Quests Completed",
              Comparator.comparingInt(this::completedQuests).reversed(),
              this::completedQuests);
    } else if (ScoreBoardMenuCommands.SortByDailyQuests.getMatcher(command) != null) {
      handleShow(
              "Daily Quests Completed",
              Comparator.comparingInt(this::completedDailyQuests).reversed(),
              this::completedDailyQuests);
    } else if (MenuCommands.ShowCurrentMenu.getMatcher(command) != null) {
      System.out.println("Leaderboard");
    } else if (MenuCommands.ExitMenu.getMatcher(command) != null) {
      exit();
    } else {
      System.out.println("invalid input");
    }
  }

  private void handleShow(
          String label, Comparator<User> comparator, java.util.function.ToIntFunction<User> valueOf) {
    List<User> ranked = new ArrayList<>(UserManager.getInstance().getAllUsers());
    if (ranked.isEmpty()) {
      System.out.println("No registered users yet.");
      return;
    }

    ranked.sort(comparator);

    System.out.println("--- Leaderboard: " + label + " ---");
    int rank = 1;
    for (User user : ranked) {
      System.out.println("  " + rank + ". " + user.getUsername() + " - " + valueOf.applyAsInt(user));
      rank++;
    }
  }

  private int meowPoints(User user) {
    return user.getMeowPoints();
  }

  private int stageLevelScore(User user) {
    return user.getProgress().getMaxClearedStage() * 100 + user.getProgress().getMaxClearedLevel();
  }

  private int miniGamesCleared(User user) {
    return user.getProgress().getUnlockedMiniGames().size();
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