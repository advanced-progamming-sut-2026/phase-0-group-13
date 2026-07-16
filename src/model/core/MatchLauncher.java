package model.core;

import data.GameDataManager;
import data.persistence.UserManager;
import java.util.ArrayList;
import java.util.List;
import model.account.User;
import model.enums.Menu;
import model.environment.AncientEgyptSeason;
import model.environment.BigWaveBeachSeason;
import model.environment.DarkAgesSeason;
import model.environment.FrostbiteCavesSeason;
import model.environment.Season;
import model.game.Wave;
import model.game.WaveGenerator;
import model.game.zombie.Zombie;

/**
 * Builds a {@link GameManager} from the current {@link MatchSetup} and hands control to the
 * gameplay menu: resolves the chapter's {@link Season}, generates its waves, and starts the match.
 */
public final class MatchLauncher {
  private static final int ROWS = 5;
  private static final int COLS = 9;
  private static final int LEVELS_PER_STAGE = 4;

  private MatchLauncher() {}

  public static void launch() {
    int stage = resolveStageNumber();
    Season season = seasonForStage(stage);

    GameManager gameManager = new GameManager();
    gameManager.initializeLevel(ROWS, COLS, buildWaves(stage, season));
    season.applySeasonEffects(gameManager.getBoard().getGameState());
    gameManager.startGame();

    GameSession.start(gameManager, Menu.GameMenu);
    App.setCurrentMenu(Menu.GamePlayMenu);

    System.out.println(
        "Battle started: "
            + season.getName()
            + " ("
            + gameManager.getTotalWaves()
            + " waves). Type 'show map', 'advance time -t <n> ticks', "
            + "'plant plant -t <type> -l (x,y)', or 'menu exit'.");
  }

  private static int resolveStageNumber() {
    String chapter = MatchSetup.getInstance().getTargetChapter();
    if (chapter == null) {
      return 1;
    }
    String normalized =
        chapter.trim().toLowerCase().replaceFirst("^(chapter|stage)\\s*[-_ ]?", "").trim();
    try {
      return Math.max(1, Integer.parseInt(normalized));
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  private static Season seasonForStage(int stage) {
    switch (stage) {
      case 1:
        return new AncientEgyptSeason();
      case 2:
        return new FrostbiteCavesSeason();
      case 3:
        return new BigWaveBeachSeason();
      default:
        return new DarkAgesSeason();
    }
  }

  private static List<Wave> buildWaves(int stage, Season season) {
    List<String> zombieNames = new ArrayList<>();
    for (Zombie zombie : season.getAvailableZombies()) {
      if (zombie.getName() != null && !zombieNames.contains(zombie.getName())) {
        zombieNames.add(zombie.getName());
      }
    }
    if (zombieNames.isEmpty() && GameDataManager.zombieRepository != null) {
      for (var template : GameDataManager.zombieRepository.getAll()) {
        if (template.getName() != null) {
          zombieNames.add(template.getName());
        }
      }
    }
    return WaveGenerator.generate(levelNumber(stage), zombieNames);
  }

  /** Overall level index across chapters, used to scale wave difficulty. */
  private static int levelNumber(int stage) {
    User user = UserManager.getInstance().getCurrentUser();
    int levelInStage = user != null ? user.getProgress().getCurrentLevel() : 1;
    return (stage - 1) * LEVELS_PER_STAGE + levelInStage;
  }
}
