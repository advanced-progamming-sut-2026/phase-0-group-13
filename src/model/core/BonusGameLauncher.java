package model.core;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import model.enums.Menu;
import model.game.Wave;
import model.game.WaveGenerator;


public final class BonusGameLauncher {
  private static final int ROWS = 5;
  private static final int COLS = 9;
  private static final int BONUS_LEVEL = 5;

  private BonusGameLauncher() {}

  public static void launch() {
    GameManager gameManager = new GameManager();
    gameManager.initializeLevel(ROWS, COLS, buildDailyWaves());
    gameManager.setBonusMatch(true);
    gameManager.startGame();

    GameSession.start(gameManager, Menu.MainMenu);
    App.setCurrentMenu(Menu.GamePlayMenu);

    System.out.println("--- Game Bonus ---");
    System.out.println(
        "Today's zombies are the same for every player. Rack up MyoPoints before they reach "
            + "your house! Type 'show map', 'advance time -t <n> ticks', "
            + "'plant plant -t <type> -l (x,y)', or 'menu exit'.");
  }

  private static List<Wave> buildDailyWaves() {
    List<String> zombieNames = new ArrayList<>();
    if (GameDataManager.zombieRepository != null) {
      for (var template : GameDataManager.zombieRepository.getAll()) {
        if (template.getName() != null && !zombieNames.contains(template.getName())) {
          zombieNames.add(template.getName());
        }
      }
    }
    return WaveGenerator.generateDailyScoreGameWaves(BONUS_LEVEL, zombieNames);
  }
}