package model.game.minigame.arcade;

public record IZombieAction(Kind kind, String type, int row, int col) {

  public enum Kind {
    PLACE_ZOMBIE,
    PLACE_PLANT
  }

  public static IZombieAction placeZombie(String type, int row, int col) {
    return new IZombieAction(Kind.PLACE_ZOMBIE, type, row, col);
  }

  public static IZombieAction placePlant(String type, int row, int col) {
    return new IZombieAction(Kind.PLACE_PLANT, type, row, col);
  }
}
