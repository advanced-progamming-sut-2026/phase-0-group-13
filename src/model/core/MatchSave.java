package model.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A match in progress, in the shape it is written to disk.
 *
 * <p>Plain fields and a no-arg constructor because Gson builds it back; nothing here is behaviour.
 *
 * <p>What is kept is the lawn and the economy -- which level it is, which plants are standing and
 * how hurt they are, how much sun and plant food the player is holding, and how far through the
 * waves they had got. Zombies already on the lawn are deliberately not kept: they belong to a wave
 * that is half-spawned, and restoring one without the wave state behind it produces a match the
 * generator then fights with. Resuming therefore drops you back at the start of the wave you were
 * on, with your lawn intact.
 */
public class MatchSave {

  /** One planted plant: what it is, where it stands, and how much health is left. */
  public static class SavedPlant {
    public String name;
    public int row;
    public int col;
    public int health;
    public int level;

    public SavedPlant() {
    }

    public SavedPlant(String name, int row, int col, int health, int level) {
      this.name = name;
      this.row = row;
      this.col = col;
      this.health = health;
      this.level = level;
    }
  }

  public String username;
  public String targetChapter;
  public int targetLevel;
  public int difficultyLevel;
  public List<String> selectedPlants = new ArrayList<>();
  public List<String> boostedPlants = new ArrayList<>();
  public List<SavedPlant> plants = new ArrayList<>();
  public int sun;
  public int plantFood;
  public int waveIndex;
  public boolean wavesStarted;
  public long savedAt;

  public MatchSave() {
  }

  /** A short line for the resume button, e.g. "Ancient Egypt - level 3". */
  public String describe() {
    String chapter = targetChapter == null || targetChapter.isBlank() ? "Adventure" : targetChapter;
    return chapter + " - level " + targetLevel;
  }
}
