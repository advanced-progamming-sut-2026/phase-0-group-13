package model.game.zombie.behavior;

import model.game.Board;
import model.game.zombie.Zombie;

public interface ZombieAction {
  void execute(Zombie zombie, Board board, int currentTick);

  default void onDeath(Zombie zombie, Board board) {}

  default String debugState(Zombie zombie, int currentTick) {
    return null;
  }
}
