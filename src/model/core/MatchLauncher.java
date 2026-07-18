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
import model.game.minigame.ConveyorRule;
import model.game.minigame.DeadLineRule;
import model.game.minigame.LockedPlantsRule;
import model.game.minigame.LoveYourPlantsRule;
import model.game.minigame.NightOpsRule;
import model.game.minigame.PlantWhatYouGetRule;
import model.game.minigame.SaveOurSeedsRule;
import model.game.minigame.SpecialStageRule;
import model.game.minigame.TimedWarRule;
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
    season.placeHazards(gameManager.getBoard());
    attachSpecialRule(gameManager, stage);
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

  /**
   * Levels 2 and 3 of each chapter are special levels; each of the 8 special types appears once
   * across the adventure. Level 1 is a normal level and level 4 is the boss (next phase).
   */
  private static void attachSpecialRule(GameManager gameManager, int stage) {
    User user = UserManager.getInstance().getCurrentUser();
    int levelInStage = user != null ? user.getProgress().getCurrentLevel() : 1;
    List<String> deck = new ArrayList<>(MatchSetup.getInstance().getSelectedPlants());

    SpecialStageRule rule = specialRuleFor(stage, levelInStage, deck);
    if (rule == null) {
      return;
    }

    gameManager.setSpecialStageRule(rule);
    System.out.println("Special level active: " + rule.getClass().getSimpleName());
    if (rule instanceof PlantWhatYouGetRule) {
      // زامبی‌ها تا وقتی بازیکن 'start zombie waves' نزنه وارد نمیشن
      gameManager.pauseZombieWaves();
      System.out.println("Plant freely, then type 'start zombie waves' to begin the assault.");
    }
  }

  private static SpecialStageRule specialRuleFor(int stage, int levelInStage, List<String> deck) {
    if (levelInStage != 2 && levelInStage != 3) {
      return null;
    }
    boolean second = levelInStage == 2;
    switch (stage) {
      case 1:
        return second
            ? new ConveyorRule(deck, 120)
            : new LockedPlantsRule(deck.subList(0, Math.max(1, deck.size() - 1)));
      case 2:
        return second ? new SaveOurSeedsRule() : new TimedWarRule(1200);
      case 3:
        return second ? new NightOpsRule() : new DeadLineRule(4);
      default:
        return second
            ? new LoveYourPlantsRule(deck.isEmpty() ? "peashooter" : deck.get(0))
            : new PlantWhatYouGetRule(deck, 120);
    }
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
