package model.game.zombie.behavior;

import model.game.Board;
import model.game.zombie.Zombie;

public interface ZombieAction {
  // هر تیک صداش میزنیم حمالی کنه
  void execute(Zombie zombie, Board board, int currentTick);

  default void onDeath(Zombie zombie, Board board) {}

  // وضعیت/تایمر توانایی ویژه برای خروجی دیباگ؛ null یعنی این رفتار چیزی برای نشان دادن ندارد
  default String debugState(Zombie zombie, int currentTick) {
    return null;
  }
}
