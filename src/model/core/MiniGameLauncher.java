package model.core;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import model.enums.Menu;
import model.enums.MiniGameType;
import model.environment.AncientEgyptSeason;
import model.environment.Season;
import model.game.Wave;
import model.game.WaveGenerator;
import model.game.zombie.Zombie;

public final class MiniGameLauncher {
  private static final int ROWS = 5;
  private static final int COLS = 9;
  private static final int COIN_REWARD_PER_LEVEL = 150;
  private static final int MEOW_POINTS_ON_WIN = 200;

  private MiniGameLauncher() {}

  public static void awardClear(model.account.User user, MiniGameType type, int level) {
    if (user == null) {
      return;
    }
    int coinReward = COIN_REWARD_PER_LEVEL * level;
    user.addCoins(coinReward);
    user.addMeowPoints(MEOW_POINTS_ON_WIN);
    System.out.println("Reward: +" + coinReward + " coins, +" + MEOW_POINTS_ON_WIN + " MyoPoints.");

    String key = type.name().toLowerCase();
    if (user.getProgress().recordMiniGameCleared(key, level)) {
      int cleared = user.getProgress().getClearedMiniGameLevel(key);
      System.out.printf("Mini-game progress: %s %d/%d levels cleared.%n",
              key, cleared, model.account.Progress.MINI_GAME_LEVELS);
      if (cleared < model.account.Progress.MINI_GAME_LEVELS) {
        System.out.println("Next up: level " + (cleared + 1) + "!");
      } else {
        System.out.println("You have mastered " + key + "!");
      }
    }

    user.triggerQuestEvent("MINIGAME_CLEAR", 1);
  }

  public static void launch() {
    MiniGameType type = MatchSetup.getInstance().getCurrentMiniGame();
    int level = MatchSetup.getInstance().getMiniGameLevel();

    // Same reason as the bonus game: with no season the renderer paints the Dark Ages night lawn
    // while sky sun keeps falling on it, because only DarkAgesSeason turns that off.
    Season season = new AncientEgyptSeason();

    GameManager gameManager = new GameManager();
    gameManager.initializeLevel(ROWS, COLS, buildWaves(level, type, season));
    season.applySeasonEffects(gameManager.getBoard().getGameState());
    gameManager.setSeason(season);
    gameManager.startGame();

    GameSession.start(gameManager, Menu.QuestMenu);
    if (type == MiniGameType.ZOMBOTANY) {
      App.setCurrentMenu(Menu.GamePlayMenu);
    } else {
      App.setCurrentMenu(Menu.MiniGameMenu);
    }

    System.out.println("--- Mini-Game Started ---");
    System.out.println("Mode: " + type + " | Level: " + level);
    System.out.println(entryMessage(type));
  }

  /**
   * Zombotany draws its own four; everything else takes the season's roster.
   *
   * <p>The non-Zombotany branch used to sweep the whole of Zombies.json, which meant a mini-game
   * could open on all four Dr. Zombosses, or on a prop with no hit points, no speed and no rig.
   */
  private static List<Wave> buildWaves(int level, MiniGameType type, Season season) {
    List<String> zombieNames = new ArrayList<>();
    if (type == MiniGameType.ZOMBOTANY) {
      if (GameDataManager.zombieRepository != null) {
        for (var template : GameDataManager.zombieRepository.getAll()) {
          String name = template.getName();
          if (name != null && name.toLowerCase().contains("zombotany")
                  && !zombieNames.contains(name)) {
            zombieNames.add(name);
          }
        }
      }
    } else {
      for (Zombie zombie : season.getAvailableZombies()) {
        if (zombie.getName() != null && !zombieNames.contains(zombie.getName())) {
          zombieNames.add(zombie.getName());
        }
      }
    }
    return WaveGenerator.generate(level, zombieNames);
  }

  private static String entryMessage(MiniGameType type) {
    return switch (type) {
      case VASEBREAKER -> "Smash every vase before the zombies inside them reach your brain. "
          + "Use 'smash vase <x> <y>'.";
      case WALLNUT_BOWLING -> "The conveyor belt delivers your nuts; plant them before the red "
          + "line. Use 'plant nut <x> <y>'.";
      case I_ZOMBIE -> "Deploy zombies to break through the lawn. "
          + "Use 'place zombie <type> <x> <y>'.";
      case BEGHOULED -> "Line up three or more matching plants to earn sun. "
          + "Use 'swap plant <x1> <y1> <x2> <y2>' and 'upgrade plant <name>'.";
      case ZOMBOTANY -> "These zombies have plant powers. Play it like a normal stage: "
          + "'plant plant -t <type> -l (x,y)', 'advance time -t <n> ticks'.";
      default -> "Type 'show map', 'advance time <n> ticks', or 'exit' to leave.";
    };
  }
}
