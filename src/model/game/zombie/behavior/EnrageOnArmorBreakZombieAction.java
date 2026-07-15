package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class EnrageOnArmorBreakZombieAction implements ZombieAction {
  private final double eatingDamage;
  private final double enragedSpeedMultiplier;
  private boolean enraged = false;

  // نیوزپیپر تا وقتی زره‌ش (روزنامه) سرپاست عادی راه میره؛ به محض نابود شدنش، برای همیشه سرعتش
  // enragedSpeedMultiplier برابر میشه (فعلا برگشت به حالت عادی نداره چون این وضعیت دائمیه)
  public EnrageOnArmorBreakZombieAction(double eatingDamage, double enragedSpeedMultiplier) {
    this.eatingDamage = eatingDamage;
    this.enragedSpeedMultiplier = enragedSpeedMultiplier;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (!enraged && !zombie.hasIntactArmor()) {
      enraged = true;
      zombie.setSpeedMultiplier(enragedSpeedMultiplier);
      System.out.printf("%s lost its newspaper and got enraged!%n", zombie.getName());
    }

    Plant targetPlant = board.getPlantAt(zombie.getRow(), zombie.getX());
    if (targetPlant != null && !targetPlant.isDead()) {
      zombie.setEating(true);
      if (currentTick % 10 == 0) {
        targetPlant.takeDamage((int) eatingDamage);
      }
    } else {
      zombie.setEating(false);
      zombie.move();
    }
  }
}
