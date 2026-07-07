package model.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.enums.Menu;
import model.game.Wave;

/**
 * Builds a {@link GameManager} from the current {@link MatchSetup} and hands control to the
 * gameplay menu. This is Person A's entry point; Person C's plant-selection "start game" calls
 * {@link #launch()} after locking in the deck.
 *
 * <p>NOTE: the level content built here is a temporary placeholder. Person B replaces {@link
 * #buildWaves()} with a real LevelFactory keyed off {@link MatchSetup#getTargetChapter()} (season,
 * tile layout, scaled waves).
 */
public final class MatchLauncher {
  private static final int ROWS = 5;
  private static final int COLS = 9;

  private MatchLauncher() {}

  public static void launch() {
    GameManager gameManager = new GameManager();
    gameManager.initializeLevel(ROWS, COLS, buildWaves());
    gameManager.startGame();

    GameSession.start(gameManager, Menu.GameMenu);
    App.setCurrentMenu(Menu.GamePlayMenu);

    System.out.println(
        "Battle started on a "
            + ROWS
            + "x"
            + COLS
            + " lawn. Type 'show map', 'advance time -t <n> ticks', "
            + "'plant plant -t <type> -l (x,y)', or 'menu exit'.");
  }

  // TODO(Person B): replace with a real LevelFactory. For now we create a single idle wave that
  // never spawns, so the runtime is playable (plant / sun / cheats / map) even before the plant &
  // zombie JSON data mapping is fixed.
  private static List<Wave> buildWaves() {
    List<Wave> waves = new ArrayList<>();
    Map<String, Integer> delays = new HashMap<>();
    delays.put("placeholder", Integer.MAX_VALUE);
    List<String> names = new ArrayList<>();
    names.add("placeholder");
    waves.add(new Wave(1, names, delays));
    return waves;
  }
}
