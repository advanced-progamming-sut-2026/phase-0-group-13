package model.game.zombie.behavior;

import model.game.Board;
import model.game.zombie.Zombie;

public interface ZombieAction {
    // هر تیک صداش میزنیم حمالی کنه
    void execute(Zombie zombie, Board board, int currentTick);
}