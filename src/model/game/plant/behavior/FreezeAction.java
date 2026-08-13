package model.game.plant.behavior;

import model.enums.StatusEffect;
import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

/**
 * گیاهان یخی‌ای که در plants.json دمیجشان صفر است و کارشان «یخ‌زدنِ» زامبی‌هاست، نه آسیب‌زدن:
 * Iceberg Lettuce اولین زامبی‌ای که رویش پا بگذارد و Ice-shroom کل زمین را یخ می‌زند.
 *
 * <p>قبلا هر دو با ExplodeAction ساخته می‌شدند و چون دمیج صفرشان به ۱۸۰۰ فالبک می‌خورد، به جای
 * یخ‌زدن، زامبی‌ها را منفجر می‌کردند.
 *
 * <p>دیتا مدت یخ‌زدگی را مشخص نکرده (فقط «Freeze Time +2s» در لِوِل ۳ آمده)، پس پایه روی
 * {@link #DEFAULT_FREEZE_SECONDS} ثانیه گذاشته شده.
 */
public class FreezeAction implements PlantAction {

  public static final int DEFAULT_FREEZE_SECONDS = 5;
  private static final int TICKS_PER_SECOND = 10;
  private static final double CONTACT_REACH = 1.0;

  private final int freezeTicks;
  private final boolean wholeLawn;
  private final boolean consumedOnUse;

  public FreezeAction(int freezeTicks, boolean wholeLawn, boolean consumedOnUse) {
    this.freezeTicks = Math.max(1, freezeTicks);
    this.wholeLawn = wholeLawn;
    this.consumedOnUse = consumedOnUse;
  }

  /** یخ‌زدگی با مدت پیش‌فرض. */
  public static FreezeAction defaultFreeze(boolean wholeLawn, boolean consumedOnUse) {
    return new FreezeAction(DEFAULT_FREEZE_SECONDS * TICKS_PER_SECOND, wholeLawn, consumedOnUse);
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    int frozen = 0;
    for (Zombie zombie : board.getZombies()) {
      if (zombie.isDead() || zombie.isHypnotized() || !inScope(plant, zombie)) {
        continue;
      }
      zombie.applyEffect(StatusEffect.FROZEN, freezeTicks);
      zombie.setEating(false);
      frozen++;
      if (!wholeLawn) {
        break;
      }
    }

    if (frozen == 0) {
      return;
    }
    System.out.printf("%s froze %d zombie(s) for %ds.%n",
            plant.getName(), frozen, freezeTicks / TICKS_PER_SECOND);
    if (consumedOnUse) {
      plant.takeDamage(plant.getMaxHealth());
    }
  }

  private boolean inScope(Plant plant, Zombie zombie) {
    if (wholeLawn) {
      return true;
    }
    return zombie.getRow() == plant.getRow()
            && Math.abs(zombie.getX() - plant.getCol()) <= CONTACT_REACH;
  }
}
