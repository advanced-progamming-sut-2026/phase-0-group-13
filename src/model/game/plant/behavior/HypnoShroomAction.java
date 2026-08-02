package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

// Hypno-shroom خودش حمله نمیکنه؛ منتظر میمونه تا زامبی شروع به خوردنش کنه و همون لحظه زامبی رو
// هیپنوتیزم میکنه (Zombie.setHypnotized که StandardZombieAction قبلا هندلش میکرد) و خودش مصرف میشه
public class HypnoShroomAction implements PlantAction {
  private static final int GARGANTUAR_HEALTH = 3000;

  private final boolean gargantuarMode;

  public HypnoShroomAction() {
    this(false);
  }

  /** حالت غذای گیاه: زامبی هیپنوتیزم‌شده به قدرت یک غول‌پیکر هم‌پیمان تبدیل می‌شود. */
  public HypnoShroomAction(boolean gargantuarMode) {
    this.gargantuarMode = gargantuarMode;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    Zombie eater = board.getZombieAt(plant.getRow(), plant.getCol());
    if (eater == null || !eater.isEating() || eater.isHypnotized()) {
      return;
    }

    eater.setHypnotized(true);
    eater.setEating(false);
    if (gargantuarMode) {
      eater.heal(GARGANTUAR_HEALTH);
      System.out.printf("%s turned %s into an allied Gargantuar!%n",
              plant.getName(), eater.getName());
    } else {
      System.out.printf("%s hypnotised %s; it now fights for you!%n",
              plant.getName(), eater.getName());
    }
    plant.takeDamage(plant.getMaxHealth());
  }
}
