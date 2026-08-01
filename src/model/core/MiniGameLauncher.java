package model.core;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import model.enums.Menu;
import model.enums.MiniGameType;
import model.game.Wave;
import model.game.WaveGenerator;

public final class MiniGameLauncher {
  private static final int ROWS = 5;
  private static final int COLS = 9;

  private MiniGameLauncher() {}

  public static void launch() {
    MiniGameType type = MatchSetup.getInstance().getCurrentMiniGame();
    int level = MatchSetup.getInstance().getMiniGameLevel();

    GameManager gameManager = new GameManager();
    gameManager.initializeLevel(ROWS, COLS, buildWaves(level, type));
    gameManager.startGame();

    GameSession.start(gameManager, Menu.QuestMenu);
    if (type == MiniGameType.ZOMBOTANY) {
      // زامبوتانی موتور جداگانه نداره: یه مرحله‌ی عادیه که استخر زامبی‌هاش گیاه-زامبی‌هان
      gameManager.setBonusMatch(true);
      App.setCurrentMenu(Menu.GamePlayMenu);
    } else {
      App.setCurrentMenu(Menu.MiniGameMenu);
    }

    System.out.println("--- Mini-Game Started ---");
    System.out.println("Mode: " + type + " | Level: " + level);
    System.out.println(entryMessage(type));
  }

  private static List<Wave> buildWaves(int level, MiniGameType type) {
    List<String> zombieNames = new ArrayList<>();
    if (GameDataManager.zombieRepository != null) {
      for (var template : GameDataManager.zombieRepository.getAll()) {
        String name = template.getName();
        if (name == null || zombieNames.contains(name)) {
          continue;
        }
        if (type == MiniGameType.ZOMBOTANY && !name.toLowerCase().contains("zombotany")) {
          continue;
        }
        zombieNames.add(name);
      }
    }
    return WaveGenerator.generate(level, zombieNames);
  }

  private static String entryMessage(MiniGameType type) {
    return switch (type) {
      case VASEBREAKER -> "Smash every vase before the zombies inside them reach your brain. "
          + "Use 'smash vase <x> <y>'.";
      case WALLNUT_BOWLING -> "Line up your shot and send a walnut down the lane. "
          + "Use 'roll walnut <lane>'.";
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