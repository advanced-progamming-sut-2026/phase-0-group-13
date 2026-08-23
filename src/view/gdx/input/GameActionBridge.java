package view.gdx.input;


/**
 * How graphical input talks to the game.
 *
 * <p>The controllers take input as one String matched against a regex enum, like
 * handleinput("plant plant -t Peashooter -l (3,2)"). Screens shouldn't be building those, because
 * a click would get turned into text just to be parsed straight back. They call this instead.
 *
 * <p>Row and column are 0-based like Board.getTile(row, col). The terminal commands are 1-based
 * and the other way round, (column, row), so the implementation converts.
 *
 * <p>Everything returns whether it actually happened: a click needs to know if a sun was taken
 * before it falls through to planting, and a tool only disarms once it has been used.
 */
public interface GameActionBridge {

  /** plantType is the template name that PlantRepository.find takes. */
  boolean plantAt(int row, int column, String plantType);

  boolean pluckAt(int row, int column);

  boolean collectSunAt(int row, int column);

  boolean feedPlantAt(int row, int column);

  /** Leaves the match and goes back to whatever screen started it. */
  void requestExit();
}
