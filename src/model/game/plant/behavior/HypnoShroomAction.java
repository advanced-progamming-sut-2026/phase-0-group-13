package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

// Hypno-shroom خودش حمله نمیکنه؛ منتظر میمونه تا زامبی شروع به خوردنش کنه و همون لحظه زامبی رو
// هیپنوتیزم میکنه (Zombie.setHypnotized که StandardZombieAction قبلا هندلش میکرد) و خودش مصرف میشه
public class HypnoShroomAction implements PlantAction {

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    Zombie eater = board.getZombieAt(plant.getRow(), plant.getCol());
    if (eater == null || !eater.isEating() || eater.isHypnotized()) {
      return;
    }

    eater.setHypnotized(true);
    eater.setEating(false);
    System.out.printf("%s hypnotised %s; it now fights for you!%n",
            plant.getName(), eater.getName());
    plant.takeDamage(plant.getMaxHealth());
  }
}
