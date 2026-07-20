package model.game;
import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import model.enums.PlantTag;
import model.enums.StatusEffect;
import model.game.TileEffects.IceTrailEffect;
import model.game.TileEffects.TileEffect;
import model.game.TileEffects.TombStoneEffect;
import model.game.plant.Plant;
import model.game.reward.Currency;
import model.game.reward.Reward;
import model.game.zombie.Zombie;
import model.game.zombie.factory.ZombieFactory;
public class Board {
  private static final double DEATH_DROP_CHANCE = 0.10;
  private final int rows;
  private final int columns;
  private Tile[][] tiles;
  private final List<Zombie> zombies;
  private final List<Plant> plants;
  private final SunManager sunManager; // کلاسی که مدیریت خورشیدها را بر عهده دارد
  private final List<Projectile> projectiles;
  private final List<Lawnmower> lawnmowers;
  private final List<Reward> pendingRewards = new ArrayList<>();
  private int pendingKillCount;
  private final List<KillDetail> pendingKillDetails = new ArrayList<>();
  private int pendingPlantsLostCount;
  public record KillDetail(int row, long column, boolean laneHasUnusedMower) {}
  private final GameState gameState;
  private final Random random;
  private boolean playerLost;
  public Board(int rows, int columns) {
    this.rows = rows;
    this.columns = columns;
    this.gameState = new GameState();
    this.zombies = new ArrayList<>();
    this.plants = new ArrayList<>();
    this.sunManager = new SunManager();
    this.projectiles = new ArrayList<>();
    this.lawnmowers = new ArrayList<>();
    this.random = new Random();
    initialize();
  }
  public void initialize() {
    tiles = new Tile[rows][columns];
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < columns; j++) {
        tiles[i][j] = new Tile();
      }
      lawnmowers.add(new Lawnmower(i));
    }
  }
  public void updateAll(int currentTick) {
    gameState.update(null, null);
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < columns; j++) {
        TileEffect effect = tiles[i][j].getEffect();
        if (effect != null) {
          effect.tick();
          if (effect instanceof TombStoneEffect tombstone) {
            tryNecromancy(tombstone, i, j, currentTick);
          }
        }
      }
    }
    applyTileHazardsToZombies();
    // مدیریت و آپدیت خورشیدها توسط SunManager انجام می‌شود
    sunManager.update(currentTick, this);
    for (Plant plant : plants) {
      plant.update(currentTick, this);
    }
    for (Zombie zombie : new ArrayList<>(zombies)) {
      zombie.update(currentTick, this);
      checkZombiePlantCollisions(zombie, currentTick);
    }
    handleProjectiles(currentTick);
    handleLawnmowers();
    triggerDeathExplosions();
    handleGlowingZombieDrops();
    handleDeathDrops();
    cleanupEntities();
  }
  private void tryNecromancy(TombStoneEffect tombstone, int row, int col, int currentTick) {
    if (!tombstone.isDueForNecromancy(currentTick)) {
      return;
    }
    ZombieFactory factory = new ZombieFactory(GameDataManager.zombieRepository);
    Zombie risen = factory.createZombie("ZombieEgyptImpDefault", row, col);
    if (risen != null) {
      spawnZombie(risen);
      System.out.printf("A zombie rises from a grave at (%d, %d)!%n", col + 1, row + 1);
    }
    tombstone.markRaised(currentTick);
  }
  private void applyTileHazardsToZombies() {
    for (Zombie zombie : zombies) {
      if (zombie.isDead()) {
        continue;
      }
      int col = (int) Math.round(zombie.getX());
      if (col < 0 || col >= columns || zombie.getRow() < 0 || zombie.getRow() >= rows) {
        continue;
      }
      TileEffect effect = tiles[zombie.getRow()][col].getEffect();
      if (effect instanceof IceTrailEffect ice && ice.isActive()) {
        if (ice.isFullFreeze()) {
          zombie.applyEffect(StatusEffect.FROZEN, 5);
        } else {
          zombie.applyEffect(StatusEffect.CHILLED, 5);
        }
      }
    }
  }
  private final model.game.plant.behavior.ExplodeAction deathExplodeAction =
          new model.game.plant.behavior.ExplodeAction(0, 1800, 1);
  private void triggerDeathExplosions() {
    for (Plant plant : plants) {
      if (plant.isDead()
              && !plant.hasDeathHookFired()
              && plant.getTags().contains(PlantTag.EXPLOSIVE)) {
        plant.markDeathHookFired();
        deathExplodeAction.detonateNow(plant, this);
      }
    }
  }
  private void handleGlowingZombieDrops() {
    for (Zombie zombie : zombies) {
      if (zombie.isDead() && zombie.isShiny() && !zombie.hasDroppedPlantFood()) {
        zombie.markPlantFoodDropped();
        if (gameState.addPlantFood()) {
          System.out.printf(
                  "The glowing zombie dropped a plant food; you have %d plant foods now.%n",
                  gameState.getPlantFoodCount());
        }
      }
    }
  }
  private void handleDeathDrops() {
    for (Zombie zombie : zombies) {
      if (zombie.isDead() && !zombie.hasDroppedLoot()) {
        zombie.markLootDropped();
        pendingKillCount++;
        pendingKillDetails.add(new KillDetail(
                zombie.getRow(), Math.round(zombie.getX()), laneHasUnusedMower(zombie.getRow())));
        if (random.nextDouble() < DEATH_DROP_CHANCE) {
          rollDeathDrop(zombie);
        }
      }
    }
  }
  private boolean laneHasUnusedMower(int row) {
    if (row < 0 || row >= lawnmowers.size()) {
      return false;
    }
    return lawnmowers.get(row).isActive();
  }
  private void rollDeathDrop(Zombie zombie) {
    int roll = random.nextInt(3);
    Reward reward;
    String dropName;
    if (roll == 0) {
      reward = new Currency("COIN", 10);
      dropName = "a coin";
    } else if (roll == 1) {
      reward = new Currency("DIAMOND", 1);
      dropName = "a diamond";
    } else {
      reward = new Currency("COIN", 25);
      dropName = "a pot of coins";
    }
    pendingRewards.add(reward);
    System.out.printf(
            "Zombie of type %s dropped %s at (%d, %d).%n",
            zombie.getName(), dropName, Math.round(zombie.getX()), zombie.getRow());
  }
  public List<Reward> drainPendingRewards() {
    List<Reward> drained = new ArrayList<>(pendingRewards);
    pendingRewards.clear();
    return drained;
  }
  public int drainPendingKillCount() {
    int count = pendingKillCount;
    pendingKillCount = 0;
    return count;
  }
  public List<KillDetail> drainPendingKillDetails() {
    List<KillDetail> drained = new ArrayList<>(pendingKillDetails);
    pendingKillDetails.clear();
    return drained;
  }
  public void placeTileEffect(int row, int col, TileEffect effect) {
    Tile tile = getTile(row, col);
    if (tile != null) {
      tile.setEffect(effect);
    }
  }
  public void triggerGraveNecromancy(int currentTick) {
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < columns; j++) {
        TileEffect effect = tiles[i][j].getEffect();
        if (effect instanceof TombStoneEffect tombstone && tombstone.isActive()) {
          ZombieFactory factory = new ZombieFactory(GameDataManager.zombieRepository);
          Zombie risen = factory.createZombie("ZombieEgyptImpDefault", i, j);
          if (risen != null) {
            spawnZombie(risen);
            System.out.printf("A grave at (%d, %d) rises as the new wave begins!%n", j + 1, i + 1);
          }
          tombstone.markRaised(currentTick);
        }
      }
    }
  }
  private void checkZombiePlantCollisions(Zombie zombie, int currentTick) {
    if (zombie.isHypnotized()) {
      checkHypnotizedZombieCollisions(zombie, currentTick);
    }
  }
  private void checkHypnotizedZombieCollisions(Zombie zombie, int currentTick) {
    Zombie targetZombie = findNearestZombieAhead(zombie);
    if (targetZombie != null && !targetZombie.isDead()) {
      zombie.setEating(true);
      if (currentTick % 10 == 0) {
        targetZombie.takeDamage(10, false);
      }
    } else {
      zombie.setEating(false);
      zombie.move();
    }
  }

  private Zombie findNearestZombieAhead(Zombie zombie) {
    Zombie nearest = null;
    double nearestDistance = Double.MAX_VALUE;
    for (Zombie other : zombies) {
      if (other == zombie || other.isDead() || other.getRow() != zombie.getRow()) {
        continue;
      }
      double distance = other.getX() - zombie.getX();
      if (distance > 0 && distance < 0.5 && distance < nearestDistance) {
        nearest = other;
        nearestDistance = distance;
      }
    }
    return nearest;
  }

  private void handleProjectiles(int currentTick) {
    ListIterator<Projectile> iterator = projectiles.listIterator();
    while (iterator.hasNext()) {
      Projectile p = iterator.next();
      p.move();

      if (p.isFromZombie()) {
        Plant plantHere = getPlantAt(Math.round(p.getYCoordinate()), p.getXCoordinate());
        if (plantHere != null && !plantHere.isDead()) {
          p.hitPlant(plantHere, currentTick);
          iterator.remove();
          continue;
        }
        if (p.getXCoordinate() < 0) {
          iterator.remove();
        }
        continue;
      }

      if (p.getEffect() != Projectile.ProjectileEffect.FIRE) {
        Plant plantHere = getPlantAt(Math.round(p.getYCoordinate()), p.getXCoordinate());
        if (plantHere != null && plantHere.getTags().contains(PlantTag.FIRE)) {
          p = p.ignited();
          iterator.set(p);
        }
      }
      if (isBlockedByTombstone(p)) {
        iterator.remove();
        continue;
      }
      boolean hitRegistered = false;
      for (Zombie zombie : zombies) {
        if (zombie.getRow() == p.getYCoordinate()
                && Math.abs(zombie.getX() - p.getXCoordinate()) < 0.5) {
          p.hitZombie(zombie);
          hitRegistered = true;
          break;
        }
      }
      if (hitRegistered || p.getXCoordinate() > columns) {
        iterator.remove();
      }
    }
  }
  private boolean isBlockedByTombstone(Projectile p) {
    if (p.isFromZombie()) {
      return false;
    }
    int row = Math.round(p.getYCoordinate());
    int col = (int) Math.round(p.getXCoordinate());
    if (row < 0 || row >= rows || col < 0 || col >= columns) {
      return false;
    }
    TileEffect effect = tiles[row][col].getEffect();
    return effect instanceof TombStoneEffect tombstone
            && tombstone.isActive()
            && tombstone.isBlocksShots();
  }
  private void handleLawnmowers() {
    for (Lawnmower mower : lawnmowers) {
      if (!mower.isActive()) {
        for (Zombie z : zombies) {
          if (z.getRow() == mower.getRow() && z.getX() <= -0.5) {
            System.out.println("The zombie ate your brain; LOSER!!!");
            playerLost = true;
            return;
          }
        }
        continue;
      }
      for (Zombie z : zombies) {
        if (z.getRow() == mower.getRow() && z.getX() <= 0.0) {
          mower.setActive(false);
          triggerLawnmowerRow(mower.getRow());
          break;
        }
      }
    }
  }
  private void triggerLawnmowerRow(int row) {
    System.out.printf("The lawn mower in the row %d is triggered and killed these zombies:%n", row);
    for (Zombie z : zombies) {
      if (z.getRow() == row) {
        z.takeDamage(10000, true);
        System.out.println("- " + z.getName());
      }
    }
  }
  private void cleanupEntities() {
    for (Zombie zombie : zombies) {
      if (zombie.isDead()) {
        System.out.printf(
                "Zombie of type %s is dead at (%d, %d).%n",
                zombie.getName(), Math.round(zombie.getX()), zombie.getRow());
      }
    }
    for (Plant plant : plants) {
      if (plant.isDead()) {
        pendingPlantsLostCount++;
      }
    }
    plants.removeIf(Plant::isDead);
    zombies.removeIf(Zombie::isDead);
    sunManager.cleanupExpiredSuns();
  }
  public int drainPendingPlantsLostCount() {
    int count = pendingPlantsLostCount;
    pendingPlantsLostCount = 0;
    return count;
  }
  public boolean isWaterAt(int row, int col) {
    if (row < 0 || row >= rows || col < 0 || col >= columns) {
      return false;
    }
    return tiles[row][col].isWater();
  }
  public void setWaterAt(int row, int col, boolean water) {
    if (row < 0 || row >= rows || col < 0 || col >= columns) {
      return;
    }
    tiles[row][col].setWater(water);
  }
  public Plant getPlantAt(int row, double x) {
    for (Plant p : plants) {
      if (p.getRow() == row && Math.abs(p.getCol() - x) < 0.5) {
        return p;
      }
    }
    return null;
  }
  public Plant getEdiblePlantAt(int row, double x, int currentTick) {
    Plant plant = getPlantAt(row, x);
    if (plant != null && plant.isDisabled(currentTick)) {
      return null;
    }
    return plant;
  }
  public Zombie getZombieAt(int row, double x) {
    for (Zombie z : zombies) {
      if (z.getRow() == row && !z.isDead() && Math.abs(z.getX() - x) < 0.5) {
        return z;
      }
    }
    return null;
  }
  public void addProjectile(Projectile p) {
    projectiles.add(p);
  }
  public List<Projectile> getProjectiles() {
    return projectiles;
  }
  public void addSun(Sun s) {
    sunManager.addSun(s);
  }
  public void placePlant(Plant p) {
    plants.add(p);
  }
  public void spawnZombie(Zombie z) {
    zombies.add(z);
  }
  public int getRows() {
    return rows;
  }
  public int getColumns() {
    return columns;
  }
  public GameState getGameState() {
    return gameState;
  }

  public boolean isPlayerLost() {
    return playerLost;
  }

  public boolean hasZombieInRow(int row, double plantX) {
    for (Zombie zombie : zombies) {
      if (zombie.getRow() == row && !zombie.isDead() && zombie.getX() >= plantX) {
        return true;
      }
    }
    return false;
  }

  public List<Zombie> getZombies() {
    return zombies;
  }

  public Plant getPlantAhead(int row, double currentX, double distanceAhead) {
    for (Plant p : plants) {
      if (p.getRow() == row) {
        if (p.getCol() <= currentX && p.getCol() >= (currentX - distanceAhead)) {
          return p;
        }
      }
    }
    return null;
  }

  public List<Plant> getPlants() {
    return plants;
  }

  public List<Sun> getSuns() {
    return sunManager.getSuns();
  }

  public List<Lawnmower> getLawnmowers() {
    return lawnmowers;
  }

  public Tile getTile(int row, int col) {
    if (row < 0 || row >= rows || col < 0 || col >= columns) {
      return null;
    }
    return tiles[row][col];
  }

  public Integer collectSunAt(int col, int row) {
    return sunManager.collectSunAt(col, row, this);
  }

  public void applyAreaDamageToZombies(int centerCol, int centerRow, int radius, int damage) {
    for (Zombie z : zombies) {
      if (!z.isDead() && Math.abs(z.getRow() - centerRow) <= radius && Math.abs(z.getX() - centerCol) <= radius) {
        z.takeDamage(damage, false);
      }
    }
  }
  public void applyAreaDamageToPlants(int centerCol, int centerRow, int radius, int damage) {
    for (Plant p : plants) {
      if (!p.isDead() && Math.abs(p.getRow() - centerRow) <= radius && Math.abs(p.getCol() - centerCol) <= radius) {
        p.takeDamage(damage);
      }
    }
  }
  public void placeRandomTombstones(int minCount, int maxCount, int hp) {
    int count = minCount + random.nextInt(maxCount - minCount + 1);
    for (int i = 0; i < count; i++) {
      int col = 4 + random.nextInt(columns - 4);
      int row = random.nextInt(rows);
      placeTileEffect(row, col, new TombStoneEffect(hp, true));
    }
  }
}