package model.core;

import java.util.ArrayList;
import java.util.List;
import model.enums.Menu;
import model.environment.AncientEgyptSeason;
import model.environment.Season;
import model.game.Wave;
import model.game.WaveGenerator;
import model.game.zombie.Zombie;


public final class BonusGameLauncher {
  private static final int ROWS = 5;
  private static final int COLS = 9;
  private static final int BONUS_LEVEL = 5;

  private BonusGameLauncher() {}

  public static void launch() {
    // A season, even though the bonus game belongs to no chapter. Without one getSeason() is null,
    // the renderer falls through to its "dark" default and paints the Dark Ages night lawn -- with
    // sky sun still falling on it, because only DarkAgesSeason turns that off and it was never
    // applied. Naming a day season makes the lawn and the sun economy agree.
    Season season = new AncientEgyptSeason();

    GameManager gameManager = new GameManager();
    gameManager.initializeLevel(ROWS, COLS, buildDailyWaves(season));
    season.applySeasonEffects(gameManager.getBoard().getGameState());
    gameManager.setSeason(season);
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

  /**
   * The season's own roster rather than every row of Zombies.json.
   *
   * <p>Taking the whole repository put the four Dr. Zombosses and the statless props -- an arcade
   * cabinet, a push-action helper with no rig and no speed -- into the daily wave pool, so a bonus
   * run could open on four bosses at once or on something that stands still and cannot be drawn.
   */
  private static List<Wave> buildDailyWaves(Season season) {
    List<String> zombieNames = new ArrayList<>();
    for (Zombie zombie : season.getAvailableZombies()) {
      if (zombie.getName() != null && !zombieNames.contains(zombie.getName())) {
        zombieNames.add(zombie.getName());
      }
    }
    return WaveGenerator.generateDailyScoreGameWaves(BONUS_LEVEL, zombieNames);
  }
}