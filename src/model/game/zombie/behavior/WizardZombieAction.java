package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class WizardZombieAction implements ZombieAction {
  private final int curseDurationTicks;

  // به‌جای خوردن گیاه، وقتی به یه گیاه میرسه یه بار طلسمش میکنه (Plant.disableUntil) و از کنارش رد
  // میشه؛ گیاه طلسم‌شده دیگه تا پایان مدت نمیتونه کاری بکنه ولی نمیمیره
  public WizardZombieAction(int curseDurationTicks) {
    this.curseDurationTicks = curseDurationTicks;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    Plant targetPlant = board.getPlantAt(zombie.getRow(), zombie.getX());
    if (targetPlant != null && !targetPlant.isDead() && !targetPlant.isDisabled(currentTick)) {
      targetPlant.disableUntil(currentTick + curseDurationTicks);
      System.out.printf(
          "%s turned %s into a harmless sheep!%n", zombie.getName(), targetPlant.getName());
    }

    zombie.setEating(false);
    zombie.move();
  }
}
