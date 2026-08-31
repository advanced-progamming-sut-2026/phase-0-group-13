package view.gdx.input;


public interface GameActionBridge {

  boolean plantAt(int row, int column, String plantType);

  boolean pluckAt(int row, int column);

  boolean collectSunAt(int row, int column);

  boolean collectSunByHover(int row, int column);

  boolean feedPlantAt(int row, int column);

  void requestExit();
}
