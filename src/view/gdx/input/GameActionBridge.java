package view.gdx.input;


/**
 * How graphical input talks to the game.
 *
 * <p>The controllers take input as one String matched against a regex enum, like
 * handleinput("plant plant -t Peashooter -l (3,2)"). Screens shouldn't be building those, because
 * a click would get turned into text just to be parsed straight back. They call this instead.
 *
 * <p>No implementation yet, same as AssetRegistry, so screens can be written against it while we
 * work out how to hook it up to the controllers. Row and column are 0-based like
 * Board.getTile(row, col). The terminal commands are 1-based and the other way round,
 * (column, row), so whoever implements this has to convert.
 */
public interface GameActionBridge {

  /** Places a plant. plantType is the template name that PlantRepository.find takes. */
  void plantAt(int row, int column, String plantType);

  /** Removes the plant on a tile. */
  void pluckAt(int row, int column);

  /** Collects a sun on a tile, if there is one. */
  void collectSunAt(int row, int column);

  /** Uses one plant food on the plant on a tile. */
  void feedPlantAt(int row, int column);

  /** Leaves the match and goes back to whatever screen started it. */
  void requestExit();
}
