package model.game.plant.behavior;

import model.game.Board;
import model.game.TileEffects.TombStoneEffect;
import model.game.Tile;
import model.game.plant.Plant;

/**
 * Grave Buster: در plants.json کارش «نابودکردن سنگ‌قبرهاست» (ضروری در مصر باستان و دوران تاریکی).
 * قبلا با ExplodeAction ساخته می‌شد و به جای سنگ‌قبر، به زامبی‌ها دمیج می‌زد.
 *
 * <p>از همان {@link TombStoneEffect} موجود روی تایل استفاده می‌کند و متد
 * {@link TombStoneEffect#breakStone()} خودش را صدا می‌زند، پس جایزهٔ دفن‌شده (۵۰ خورشید یا غذای
 * گیاه) هم مثل حالت عادی توسط Board آزاد می‌شود.
 *
 * <p>«Eat Time -1s» در Lvl 2 یعنی زمان جویدن قبر؛ به صورت fuse روی همین اکشن اعمال می‌شود.
 */
public class GraveBusterAction implements PlantAction {

  private static final int DEFAULT_EAT_TICKS = 30;

  private final int eatTicks;
  private boolean started;

  public GraveBusterAction(int eatTicks) {
    this.eatTicks = Math.max(0, eatTicks);
  }

  public GraveBusterAction() {
    this(DEFAULT_EAT_TICKS);
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    Tile tile = board.getTile(plant.getRow(), plant.getCol());
    TombStoneEffect grave = tile != null && tile.getEffect() instanceof TombStoneEffect stone
            ? stone : null;

    if (grave == null || !grave.isActive()) {
      // بدون سنگ‌قبر کاری ندارد؛ طبق دیتا یک‌بارمصرف است، پس همان‌جا مصرف می‌شود
      System.out.printf("%s found no gravestone here and was wasted.%n", plant.getName());
      plant.takeDamage(plant.getMaxHealth());
      return;
    }

    if (!started) {
      started = true;
      plant.setLastActionTick(currentTick);
      return;
    }
    if (currentTick - plant.getLastActionTick() < eatTicks) {
      return;
    }

    grave.breakStone();
    tile.setEffect(null);
    System.out.printf("%s destroyed the gravestone at (%d, %d).%n",
            plant.getName(), plant.getCol() + 1, plant.getRow() + 1);
    plant.takeDamage(plant.getMaxHealth());
  }
}
