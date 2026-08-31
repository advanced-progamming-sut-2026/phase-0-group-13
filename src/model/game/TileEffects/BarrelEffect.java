package model.game.TileEffects;

public class BarrelEffect extends TileEffect {

  // طبق داک روی این خانه نمی‌توان گیاه کاشت
  @Override
  public boolean blocksPlanting() {
    return isActive();
  }

  private int health;
  private boolean burst;

  public BarrelEffect(int health) {
    super("Barrel", -1);
    this.health = health;
  }

  public void takeDamage(int damage) {
    if (!isActive()) return;

    this.health = Math.max(0, this.health - damage);
    System.out.println("The barrel took " + damage + " damage. Remaining HP: " + this.health);

    if (this.health <= 0) {
      System.out.println("The barrel has been smashed open!");
      remove();
    }
  }

  public boolean hasBurst() {
    return burst;
  }

  public void markBurst() {
    this.burst = true;
  }

  public int getHealth() {
    return health;
  }
}
