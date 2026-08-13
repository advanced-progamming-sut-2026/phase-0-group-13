package model.game.TileEffects;

/**
 * گودالِ به‌جامانده از انفجار Doom-shroom. طبق plants.json این گیاه «انفجاری در کل زمین دارد و یک
 * گودالِ غیرقابل کاشت به جا می‌گذارد».
 *
 * <p>دیتا برای گودال مدت‌زمانی مشخص نکرده، پس مثل سنگ‌قبر با duration = -1 ساخته می‌شود؛ یعنی
 * تا پایان مرحله می‌ماند و {@link TileEffect#tick()} آن را پاک نمی‌کند.
 */
public class CraterEffect extends TileEffect {

  public CraterEffect() {
    super("Crater", -1);
  }

  @Override
  public boolean blocksPlanting() {
    return isActive();
  }
}
