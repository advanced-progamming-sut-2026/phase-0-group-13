package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class TacklerZombieAction implements ZombieAction {

  // برخلاف زامبی معمولی، این یکی (All-Star/Footballer) اصلا وایمیسته گیاه رو نمیخوره؛ همینجوری در
  // حرکت از روش رد میشه و نابودش میکنه
  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    Plant targetPlant = board.getPlantAt(zombie.getRow(), zombie.getX());
    if (targetPlant != null && !targetPlant.isDead()) {
      targetPlant.takeDamage(10000);
      System.out.printf("%s tackled through %s!%n", zombie.getName(), targetPlant.getName());
    }

    zombie.setEating(false);
    zombie.move();
  }
}
