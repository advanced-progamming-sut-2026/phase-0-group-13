package model.core;

import data.GameDataManager;
import data.persistence.DataPath;
import data.persistence.JsonSerializer;
import data.persistence.UserManager;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import model.account.User;
import model.game.Board;
import model.game.plant.Factory.PlantFactory;
import model.game.plant.Plant;

/**
 * Saving a match in progress and putting it back.
 *
 * <p>One save per account, in {@code data/database/Games.json} -- the path {@link DataPath} has
 * always registered under "games" and nothing ever wrote to. Starting a fresh level or finishing
 * the saved one clears it, so the resume button never offers a match that is already over.
 *
 * <p>Restoring goes through {@link MatchLauncher} rather than rebuilding a board by hand: the
 * level's own season, rules and waves are then set up exactly as a normal run, and only the lawn
 * and the economy are painted back on top. See {@link MatchSave} for what is and is not kept.
 */
public final class MatchSaveManager {

  private static final JsonSerializer SERIALIZER = new JsonSerializer();

  private MatchSaveManager() {
  }

  private static String path() {
    Path registered = DataPath.getInstance().getPath("games");
    return registered != null ? registered.toString() : "data/database/Games.json";
  }

  /** The signed-in player's saved match, or null when there is none. */
  public static MatchSave load() {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      return null;
    }
    MatchSave saved = SERIALIZER.readFromFile(path(), MatchSave.class);
    if (saved == null || saved.username == null
        || !saved.username.equalsIgnoreCase(user.getUsername())) {
      return null;
    }
    return saved;
  }

  public static boolean hasSave() {
    return load() != null;
  }

  /**
   * Drops the saved match, but only when it belongs to the player who is signed in.
   *
   * <p>Called whenever a level starts or ends, which two different accounts on one machine both
   * do: without the ownership test, simply playing a level on account B would delete account A's
   * saved match.
   */
  public static void clear() {
    if (load() == null) {
      return;
    }
    java.io.File file = new java.io.File(path());
    if (file.exists()) {
      file.delete();
    }
  }

  /**
   * Writes the match down.
   *
   * <p>Refuses anything that is not an ordinary adventure level: a bonus run is a daily score
   * attempt and a mini-game is short by design, so neither is worth resuming half-way.
   *
   * @return true when a save was actually written
   */
  public static boolean save(GameManager match) {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null || match == null || match.getBoard() == null || !match.isRunning()) {
      return false;
    }
    MatchSetup setup = MatchSetup.getInstance();
    if (match.isBonusMatch() || setup.getCurrentMiniGame() != model.enums.MiniGameType.NONE) {
      return false;
    }
    // Once every wave has been dispatched the match is in its endgame, and since a save does not
    // carry the zombies still walking, resuming from here would find an empty lawn with no waves
    // left to send and hand out a free win. Finish the level instead.
    if (match.getCurrentWaveIndex() >= match.getTotalWaves()) {
      return false;
    }

    MatchSave saved = new MatchSave();
    saved.username = user.getUsername();
    saved.targetChapter = setup.getTargetChapter();
    saved.targetLevel = setup.getTargetLevel();
    saved.difficultyLevel = setup.getDifficultyLevel();
    saved.selectedPlants = new ArrayList<>(setup.getSelectedPlants());
    saved.boostedPlants = new ArrayList<>(setup.getBoostedPlants());
    saved.sun = match.getSunAmount();
    saved.plantFood = match.getPlantFoodCount();
    saved.waveIndex = match.getCurrentWaveIndex();
    saved.wavesStarted = match.isZombieWavesStarted();
    saved.savedAt = System.currentTimeMillis();
    for (model.game.Lawnmower mower : match.getBoard().getLawnmowers()) {
      saved.mowersAvailable.add(mower.isAvailable());
    }

    for (Plant plant : match.getBoard().getPlants()) {
      if (plant != null && !plant.isDead()) {
        saved.plants.add(new MatchSave.SavedPlant(plant.getName(), plant.getRow(), plant.getCol(),
            plant.getCurrentHealth(), Math.max(1, user.getPlantLevel(plant.getName()))));
      }
    }
    SERIALIZER.writeToFile(path(), saved);
    return true;
  }

  /**
   * Rebuilds the saved match and makes it the active one.
   *
   * @return the running match, or null when it could not be restored
   */
  public static GameManager resume() {
    MatchSave saved = load();
    if (saved == null) {
      return null;
    }
    MatchSetup setup = MatchSetup.getInstance();
    setup.setTargetChapter(saved.targetChapter);
    setup.setTargetLevel(saved.targetLevel);
    setup.setDifficultyLevel(saved.difficultyLevel);
    setup.setSelectedPlants(saved.selectedPlants);
    setup.setBoostedPlants(saved.boostedPlants);

    MatchLauncher.launch();
    GameManager match = GameSession.getActiveGame();
    if (match == null) {
      return null;
    }
    restoreBoard(match, saved);
    clear();
    return match;
  }

  private static void restoreBoard(GameManager match, MatchSave saved) {
    Board board = match.getBoard();
    if (board == null) {
      return;
    }
    match.resumeAtWave(saved.waveIndex);
    if (!saved.wavesStarted) {
      match.pauseZombieWaves();
    }
    // Plants first, counters second: replanting must not be able to move the numbers the save is
    // the authority on.
    replantAll(match, saved.plants);
    restoreMowers(board, saved);
    board.getGameState().setSun(saved.sun);
    board.getGameState().setPlantFood(saved.plantFood);
  }

  /**
   * Burns back the mowers that had already been spent.
   *
   * <p>A fresh board builds a full set, so without this a player could bank three used mowers by
   * saving and resuming.
   */
  private static void restoreMowers(Board board, MatchSave saved) {
    if (saved.mowersAvailable == null) {
      return;
    }
    List<model.game.Lawnmower> mowers = board.getLawnmowers();
    for (int row = 0; row < mowers.size() && row < saved.mowersAvailable.size(); row++) {
      if (!Boolean.TRUE.equals(saved.mowersAvailable.get(row))) {
        mowers.get(row).setActive(false);
      }
    }
  }

  /**
   * Puts the saved lawn back.
   *
   * <p>Deliberately {@link Board#placePlant} and not {@link GameManager#placePlant}: the latter is
   * the *placement* path, and running it here would charge the player for plants they had already
   * bought, re-check the stage's plant rules (a locked-plants level rejects its own saved lawn
   * outright, which silently restored nothing at all), and fire onPlantPlaced again, inflating the
   * quest and statistics counters. A restore is not a placement -- these plants were legal when
   * they went down, so they simply go back where they were.
   */
  private static void replantAll(GameManager match, List<MatchSave.SavedPlant> plants) {
    Board board = match.getBoard();
    if (board == null || GameDataManager.plantRepository == null) {
      return;
    }
    PlantFactory factory = new PlantFactory(GameDataManager.plantRepository);
    for (MatchSave.SavedPlant entry : plants) {
      if (entry == null || entry.row < 0 || entry.row >= board.getRows()
          || entry.col < 0 || entry.col >= board.getColumns()) {
        continue;
      }
      Plant plant = factory.createPlant(entry.name, entry.row, entry.col, entry.level);
      if (plant == null || board.getPlantAt(entry.row, entry.col) != null) {
        continue;
      }
      board.placePlant(plant);
      // Put the wear back on: a plant that was nearly eaten should not come back untouched.
      int damage = plant.getCurrentHealth() - entry.health;
      if (damage > 0) {
        plant.takeDamage(damage);
      }
    }
  }
}
