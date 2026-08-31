package model.game.zombie.behavior;

import model.game.Board;
import model.game.TileEffects.BarrelEffect;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

// Barrel Roller بشکه رو جلوی خودش هل میده: تا وقتی بشکه (لایه‌ی armor) سالمه گیاه‌های سر راه رو له
// میکنه و شلیک‌ها به خودش نمیرسن. با شکستن بشکه دو تا imp بیرون میان و خودش مثل زامبی معمولی ادامه
// میده. اگه قبل از شکستن بشکه بمیره، بشکه سر جاش رو زمین میمونه (onDeath)
public class BarrelRollerZombieAction implements ZombieAction {
  private static final int LEFTOVER_BARREL_HEALTH = 400;

  private final double eatingDamage;
  private boolean barrelBurst;

  public BarrelRollerZombieAction(double eatingDamage) {
    this.eatingDamage = eatingDamage;
  }

  /** True once the barrel has burst and the imps are out. Read-only, for the renderer. */
  public boolean isBarrelBurst() {
    return barrelBurst;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (!barrelBurst && !zombie.hasIntactArmor()) {
      barrelBurst = true;
      board.spawnImpsFromBarrel(zombie.getRow(), zombie.getX());
    }

    if (!barrelBurst) {
      Plant crushed = board.getPlantAt(zombie.getRow(), zombie.getX());
      if (crushed != null && !crushed.isDead()) {
        crushed.takeDamage(10000);
        System.out.printf("%s's barrel rolled straight over %s!%n",
                zombie.getName(), crushed.getName());
      }
      zombie.setEating(false);
      zombie.move();
      return;
    }

    Plant targetPlant = board.getEdiblePlantAt(zombie.getRow(), zombie.getX(), currentTick);
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

  @Override
  public void onDeath(Zombie zombie, Board board) {
    if (barrelBurst) {
      return;
    }
    int col = (int) Math.round(zombie.getX());
    board.placeTileEffect(zombie.getRow(), col, new BarrelEffect(LEFTOVER_BARREL_HEALTH));
    System.out.printf("%s went down, but its barrel is still sitting at (%d, %d).%n",
            zombie.getName(), col + 1, zombie.getRow() + 1);
  }
}
