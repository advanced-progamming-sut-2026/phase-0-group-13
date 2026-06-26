package model.game.TileEffects;

public class TombStoneEffect extends TileEffect {
  private int health;
  private boolean blocksShots;

  public TombStoneEffect(int health, boolean blocksShots) {
    super("Tombstone", -1);
    this.health = health;
    this.blocksShots = blocksShots;
  }

  public void takeDamage(int damage) {
    if (!isActive()) return;

    this.health = Math.max(0, this.health - damage);
    System.out.println("Tombstone took " + damage + " damage. Remaining HP: " + this.health);

    if (this.health <= 0) {
      breakStone();
    }
  }

  public void breakStone() {
    System.out.println("The Tombstone has been completely destroyed!");
    this.blocksShots = false;
    remove();
  }

  public boolean isBlocksShots() {
    return blocksShots;
  }

  public int getHealth() {
    return health;
  }
}
