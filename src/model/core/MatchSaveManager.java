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

  public static void clear() {
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
    // The launcher hands out a starting sun float; the save is the authority on both counters.
    board.getGameState().setSun(saved.sun);
    board.getGameState().setPlantFood(saved.plantFood);
    replantAll(match, saved.plants);
  }

  private static void replantAll(GameManager match, List<MatchSave.SavedPlant> plants) {
    if (GameDataManager.plantRepository == null) {
      return;
    }
    PlantFactory factory = new PlantFactory(GameDataManager.plantRepository);
    for (MatchSave.SavedPlant entry : plants) {
      Plant plant = factory.createPlant(entry.name, entry.row, entry.col, entry.level);
      if (plant == null) {
        continue;
      }
      if (match.placePlant(plant, entry.row, entry.col)) {
        // Put the wear back on: a plant that was nearly eaten should not come back untouched.
        int damage = plant.getCurrentHealth() - entry.health;
        if (damage > 0) {
          plant.takeDamage(damage);
        }
      }
    }
  }
}
