package model.game.zombie.behavior;

import model.enums.StatusEffect;
import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

/**
 * زامبی اکتشافگر (مصر باستان): یک دینامیت پشت خود دارد که بعد از ۱۰ ثانیه منفجر می‌شود؛ بعد از
 * انفجار، دینامیت به انتهای سطر پرتاب می‌شود و خلاف جهت بقیه زامبی‌ها حرکت کرده و گیاهان را
 * از بین می‌برد. اگر تیر یخی به زامبی بخورد، فتیله خاموش می‌شود.
 */
public class ProspectorZombieAction implements ZombieAction {
  private static final int BLAST_RANGE = 1;
  private static final int BLAST_DAMAGE = 1800;
  private static final double DYNAMITE_SPEED = 0.08;
  private static final double CRUSH_RANGE = 0.6;

  private final int fuseTicks;
  private final double eatingDamage;

  private int fuseStartTick = -1;
  private boolean fuseLit = true;
  private boolean detonated;
  private double dynamiteX = -1;

  public ProspectorZombieAction(int fuseTicks, double eatingDamage) {
    this.fuseTicks = Math.max(1, fuseTicks);
    this.eatingDamage = eatingDamage;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (fuseLit && !detonated) {
      handleFuse(zombie, board, currentTick);
    }
    if (detonated) {
      advanceDynamite(zombie, board);
    }
    walkOrEat(zombie, board, currentTick);
  }

  private void handleFuse(Zombie zombie, Board board, int currentTick) {
    // تیر یخی فتیله را خاموش می‌کند
    if (zombie.getActiveEffects().containsKey(StatusEffect.CHILLED)
            || zombie.getActiveEffects().containsKey(StatusEffect.FROZEN)) {
      fuseLit = false;
      System.out.printf("%s's dynamite fuse was put out by the ice!%n", zombie.getName());
      return;
    }

    if (fuseStartTick == -1) {
      fuseStartTick = currentTick;
      return;
    }
    if (currentTick - fuseStartTick < fuseTicks) {
      return;
    }

    detonated = true;
    board.applyAreaDamageToZombies(
            (int) Math.round(zombie.getX()), zombie.getRow(), BLAST_RANGE, BLAST_DAMAGE);
    dynamiteX = board.getColumns() - 1.0;
    System.out.printf("%s's dynamite exploded and was flung to the end of row %d!%n",
            zombie.getName(), zombie.getRow() + 1);
  }

  /** دینامیت خلاف جهت زامبی‌ها (به سمت راست) حرکت می‌کند و گیاهان سر راه را نابود می‌کند. */
  private void advanceDynamite(Zombie zombie, Board board) {
    if (dynamiteX < 0 || dynamiteX >= board.getColumns()) {
      return;
    }
    dynamiteX += DYNAMITE_SPEED;

    for (Plant plant : board.getPlants()) {
      if (plant.isDead() || plant.getRow() != zombie.getRow()) {
        continue;
      }
      if (Math.abs(plant.getCol() - dynamiteX) <= CRUSH_RANGE) {
        plant.takeDamage(plant.getMaxHealth());
        System.out.printf("The rolling dynamite destroyed %s!%n", plant.getName());
        return;
      }
    }
  }

  private void walkOrEat(Zombie zombie, Board board, int currentTick) {
    Plant target = board.getEdiblePlantAt(zombie.getRow(), zombie.getX(), currentTick);
    if (target != null && !target.isDead()) {
      zombie.setEating(true);
      if (currentTick % 10 == 0) {
        target.takeDamage((int) eatingDamage);
      }
      return;
    }
    zombie.setEating(false);
    zombie.move();
  }
}
