package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class ReflectDamageAction implements PlantAction {
  private int lastObservedHealth = -1;

  // چون Plant وقتی بمیره دیگه update صدا زده نمیشه، اینجا فقط دمیج غیرکشنده رو تشخیص میدیم: هر تیک
  // میزان افت جون نسبت به تیک قبل رو حساب میکنیم و همونقدر به زامبی‌ای که همون لحظه رو این خونه اس
  // برمیگردونیم (Endurian)
  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (lastObservedHealth == -1) {
      lastObservedHealth = plant.getCurrentHealth();
      return;
    }

    int damageTaken = lastObservedHealth - plant.getCurrentHealth();
    if (damageTaken > 0) {
      Zombie attacker = board.getZombieAt(plant.getRow(), plant.getCol());
      if (attacker != null) {
        attacker.takeDamage(damageTaken, false);
      }
    }

    lastObservedHealth = plant.getCurrentHealth();
  }
}
