package model.core;

import data.GameDataManager;
import data.persistence.UserManager;
import java.util.ArrayList;
import java.util.List;
import model.account.AdventureMap;
import model.account.User;
import model.enums.Menu;
import model.environment.AncientEgyptSeason;
import model.environment.BigWaveBeachSeason;
import model.environment.DarkAgesSeason;
import model.environment.FrostbiteCavesSeason;
import model.environment.Season;
import model.game.Wave;
import model.game.WaveGenerator;
import model.game.minigame.BossStageRule;
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
import model.game.zombie.behavior.ZombossAction;

/**
 * Builds a {@link GameManager} from the current {@link MatchSetup} and hands control to the
 * gameplay menu: resolves the chapter's {@link Season}, generates its waves, and starts the match.
 */
public final class MatchLauncher {
  private static final int ROWS = 5;
  private static final int COLS = 9;
  private static final int LEVELS_PER_STAGE = AdventureMap.LEVELS_PER_STAGE;

  private MatchLauncher() {}

  public static void launch() {
    int stage = stageNumber();
    Season season = seasonForStage(stage);

    GameManager gameManager = new GameManager();
    gameManager.initializeLevel(ROWS, COLS, buildWaves(stage, season));
    season.applySeasonEffects(gameManager.getBoard().getGameState());
    season.placeHazards(gameManager.getBoard());
    gameManager.setSeason(season);
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
    int levelInStage = levelInStage();
    List<String> deck = new ArrayList<>(MatchSetup.getInstance().getSelectedPlants());
    String bossName = gameManager.getSeason() == null ? null : gameManager.getSeason().getBossZombieName();

    SpecialStageRule rule = levelInStage == BOSS_LEVEL_IN_STAGE && bossName != null
            ? new BossStageRule(bossName, deck)
            : specialRuleFor(gameManager, stage, levelInStage, deck);
    if (rule == null) {
      return;
    }

    gameManager.setSpecialStageRule(rule);
    System.out.println("Special level active: " + rule.getClass().getSimpleName());
    // Boss stages run on the belt too, so this reads the rule's belt rather than its type.
    ConveyorRule conveyor = rule.belt();
    if (conveyor != null) {
      gameManager.enableFreePlanting();
      gameManager.disableCooldowns();
      gameManager.getBoard().getGameState().setSkySunDisabled(true);
      conveyor.deliverNow();
    }
    if (rule instanceof SaveOurSeedsRule) {
      placeProtectedPlants(gameManager);
    }
    if (rule instanceof PlantWhatYouGetRule) {
      // زامبی‌ها تا وقتی بازیکن 'start zombie waves' نزنه وارد نمیشن
      gameManager.pauseZombieWaves();
      gameManager.getBoard().getGameState()
              .addSun(800 - gameManager.getBoard().getGameState().getCurrentSun());
      gameManager.getBoard().getGameState().setSkySunDisabled(true);
      gameManager.disableCooldowns();
      System.out.println("Plant freely, then type 'start zombie waves' to begin the assault.");
    }
  }

  private static void placeProtectedPlants(GameManager gameManager) {
    if (GameDataManager.plantRepository == null) {return;}
    var factory = new model.game.plant.Factory.PlantFactory(GameDataManager.plantRepository);
    for (int row = 0; row < ROWS; row += 2) {
      var guarded = factory.createPlant("sunflower", row, 0);
      if (guarded != null) {
        gameManager.getBoard().placePlant(guarded);
        System.out.printf("PROTECT THIS: %s at (1, %d)%n", guarded.getName(), row + 1);}}}
  private static final int BOSS_LEVEL_IN_STAGE = AdventureMap.LEVELS_PER_STAGE;

  /** How many ordinary waves come before Zomboss rolls on. */
  private static final int BOSS_WARMUP_WAVES = 2;

  /**
   * Which level of the chapter is being played. The map screen puts the clicked level in
   * MatchSetup; without one -- the typed menu -- it is wherever the account has got to.
   */
  public static int levelInStage() {
    int chosen = MatchSetup.getInstance().getTargetLevel();
    if (chosen > 0) {
      return chosen;
    }
    User user = UserManager.getInstance().getCurrentUser();
    return user != null ? user.getProgress().getCurrentLevel() : 1;
  }

  private static SpecialStageRule specialRuleFor(
          GameManager gameManager, int stage, int levelInStage, List<String> deck) {
    if (levelInStage != 2 && levelInStage != 3) {
      return null;
    }
    boolean second = levelInStage == 2;
    switch (stage) {
      case 1:
        return second
                ? new ConveyorRule(deck, 120)
                : new LockedPlantsRule("Pea", "peashooter");
      case 2:
        return second ? new SaveOurSeedsRule() : new TimedWarRule(1200);
      case 3:
        return second ? new NightOpsRule() : new DeadLineRule(4);
      default:
        return second
                ? (gameManager == null ? null : new LoveYourPlantsRule(5, gameManager.getMatchContext()))
                : new PlantWhatYouGetRule(deck, 120);
    }
  }

  /**
   * How many plants the seed bank needs before the stage will start. Normally MIN_DECK_SLOTS, but
   * a stage that locks most plants out can't ask for more than it allows. Shared by the typed and
   * the graphical Plant Selection menus so they can't disagree.
   */
  public static int requiredDeckSlots(User user) {
    if (user == null) {
      return 0;
    }
    SpecialStageRule rule = selectionRule();
    int selectable = (int) user.getUnlockedPlants().stream()
            .filter(name -> rule == null || rule.isPlantAllowed(name)).count();
    return Math.min(User.MIN_DECK_SLOTS, selectable);
  }

  public static SpecialStageRule selectionRule() {
    // A mini-game and the bonus run are both outside the adventure, so no stage locks their deck.
    if (MatchSetup.getInstance().getCurrentMiniGame() != model.enums.MiniGameType.NONE
            || MatchSetup.getInstance().isBonusRun()) {
      return null;
    }
    SpecialStageRule rule =
            specialRuleFor(null, stageNumber(), levelInStage(), List.of());
    return rule != null && rule.restrictsSelection() ? rule : null;
  }

  /** The chapter being played, from whatever the menus put in MatchSetup. */
  public static int stageNumber() {
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

  private static final int EARLY_LEVEL_MAX_ZOMBIE_HP = 600;
  private static final int EARLY_LEVEL_THRESHOLD = 2;

  private static List<Wave> buildWaves(int stage, Season season) {
    int level = levelNumber(stage);
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

    List<String> pool = filterPoolForLevel(level, season, zombieNames);
    List<Wave> waves = WaveGenerator.generate(level, pool);

    // مرحله‌ی آخر هر فصل با یه موج اضافه که فقط زامباس توشه تموم میشه
    if (levelInStage() == BOSS_LEVEL_IN_STAGE && season.getBossZombieName() != null) {
      // The doc replaces the wave meter with Zomboss's own health bar on these stages, so the
      // health bar is the progress readout -- which it cannot be behind ten ordinary waves the
      // player has no meter for. A short warm-up, then the boss.
      if (waves.size() > BOSS_WARMUP_WAVES) {
        waves = new ArrayList<>(waves.subList(0, BOSS_WARMUP_WAVES));
      }
      List<Wave.SpawnEntry> spawns = new ArrayList<>();
      // Zomboss stands in two lanes, so its top lane has to leave room for the second one.
      int bossLane = Math.max(0, Math.min(ROWS / 2, ROWS - ZombossAction.ROW_SPAN));
      spawns.add(new Wave.SpawnEntry(season.getBossZombieName(), bossLane, 0, 1000));
      waves.add(new Wave(waves.size() + 1, true, spawns));
    }
    return waves;
  }

  /** Intro levels should not spawn boss-tier zombies (Gargantuar, mechs, ...). */
  private static List<String> filterPoolForLevel(
      int level, Season season, List<String> zombieNames) {
    if (level > EARLY_LEVEL_THRESHOLD) {
      return zombieNames;
    }
    List<String> gentle = new ArrayList<>();
    for (Zombie zombie : season.getAvailableZombies()) {
      if (zombie.getName() != null
          && zombie.getMaxHealth() <= EARLY_LEVEL_MAX_ZOMBIE_HP
          && !gentle.contains(zombie.getName())) {
        gentle.add(zombie.getName());
      }
    }
    return gentle.isEmpty() ? zombieNames : gentle;
  }

  /**
   * The Conveyor Belt special level (chapter 1, level 2) skips plant selection entirely: the belt
   * delivers plants on its own, so entering it launches the battle directly.
   */
  public static boolean skipsPlantSelection(int stage, int levelInStage) {
    return stage == 1 && levelInStage == 2;
  }

  /** Overall level index across chapters, used to scale wave difficulty. */
  private static int levelNumber(int stage) {
    return (stage - 1) * LEVELS_PER_STAGE + levelInStage();
  }
}