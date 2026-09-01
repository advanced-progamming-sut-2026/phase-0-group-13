package model.game;
import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import model.enums.PlantTag;
import model.enums.StatusEffect;
import model.game.TileEffects.BarrelEffect;
import model.game.TileEffects.FireEffect;
import model.game.TileEffects.IceTrailEffect;
import model.game.TileEffects.TileEffect;
import model.game.TileEffects.TombStoneEffect;
import model.game.plant.Plant;
import model.game.reward.Reward;
import model.game.zombie.Zombie;
import model.game.zombie.factory.ZombieFactory;
public class Board {
  private final int rows;
  private final int columns;
  private Tile[][] tiles;
  private final List<Zombie> zombies;
  private final List<Plant> plants;
  private final SunManager sunManager;
  private final List<Projectile> projectiles;
  private final List<Lawnmower> lawnmowers;
  private final LootDropper lootDropper;
  private static final int MAX_PENDING_NOTICES = 8;
  private final List<String> pendingNotices = new ArrayList<>();
  private int pendingKillCount;
  private final List<KillDetail> pendingKillDetails = new ArrayList<>();
  private int pendingPlantsLostCount;
  private int pendingFastZombieKills;
  private int pendingMultiKillShots;
  private static final double FAST_ZOMBIE_SPEED_THRESHOLD = 0.0185;
  private final java.util.Set<Zombie> mowerVictims =
          java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
  public record KillDetail(int row, long column, boolean laneHasUnusedMower, boolean killedByMower) {}
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
    this.lootDropper = new LootDropper(this.random);
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
            tryGraveReward(tombstone, i, j);
          }
          if (effect instanceof BarrelEffect barrel) {
            tryBarrelBurst(barrel, i, j);
          }
          if (effect instanceof FireEffect fire && fire.isActive()) {
            burnPlantsOn(i, j);
          }
        }
      }
    }
    applyTileHazardsToZombies();
    sunManager.update(currentTick, this);
    for (Plant plant : plants) {
      plant.update(currentTick, this);
    }
    for (Zombie zombie : new ArrayList<>(zombies)) {
      zombie.update(currentTick, this);
      checkZombiePlantCollisions(zombie, currentTick);
    }
    retireDepartedHypnotizedZombies();
    handleProjectiles(currentTick);
    handleLawnmowers();
    triggerDeathExplosions();
    handleGlowingZombieDrops();
    handleDeathDrops();
    cleanupEntities();
  }
  private void burnPlantsOn(int row, int col) {
    for (Plant plant : plants) {
      if (!plant.isDead() && plant.getRow() == row && plant.getCol() == col) {
        plant.takeDamage(FireEffect.DAMAGE_PER_TICK);
      }
    }
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
  private void tryGraveReward(TombStoneEffect tombstone, int row, int col) {
    if (tombstone.isActive() || tombstone.getBuriedReward() == null) {
      return;
    }
    String reward = tombstone.claimReward();
    if ("SUN".equals(reward)) {
      gameState.addSun(50);
      System.out.printf("The grave at (%d, %d) held 50 sun!%n", col + 1, row + 1);
    } else if (gameState.addPlantFood()) {
      notify(String.format(
              "The grave at (%d, %d) held a plant food; you have %d plant foods now.",
              col + 1, row + 1, gameState.getPlantFoodCount()));
    }
  }

  private void tryBarrelBurst(BarrelEffect barrel, int row, int col) {
    if (barrel.isActive() || barrel.hasBurst()) {
      return;
    }
    barrel.markBurst();
    spawnImpsFromBarrel(row, col);
  }

  public void spawnImpsFromBarrel(int row, double col) {
    ZombieFactory factory = new ZombieFactory(GameDataManager.zombieRepository);
    for (int i = 0; i < 2; i++) {
      Zombie imp = factory.createZombie("ZombieEgyptImpDefault", row, col);
      if (imp != null) {
        spawnZombie(imp);
      }
    }
    System.out.printf("The barrel burst open at (%d, %d) and two imps tumbled out!%n",
            (int) Math.round(col) + 1, row + 1);
  }
  private static final int ICE_TILE_FREEZE_TICKS = 40;

  private void applyTileHazardsToZombies() {
    for (Zombie zombie : zombies) {
      if (zombie.isDead()) {
        continue;
      }
      if (zombie.isBoss()) {
        continue;
      }
      int col = (int) Math.round(zombie.getX());
      if (col < 0 || col >= columns || zombie.getRow() < 0 || zombie.getRow() >= rows) {
        continue;
      }
      TileEffect effect = tiles[zombie.getRow()][col].getEffect();
      boolean onFrozenTile = false;
      if (effect instanceof IceTrailEffect ice && ice.isActive()) {
        if (ice.getSlideDirection() != 0) {
          slideZombie(zombie, ice.getSlideDirection());
        } else if (ice.isFullFreeze()) {
          onFrozenTile = true;
          freezeOnIceTile(zombie, zombie.getRow() * columns + col);
        } else if (ice.isSlippery()) {
          slideZombie(zombie, ice.getLaneShift());
        } else {
          zombie.applyEffect(StatusEffect.CHILLED, 5);
        }
      }
      if (!onFrozenTile) {
        zombie.setIcedOnCell(Zombie.NO_CELL);
      }
    }
  }

  private void freezeOnIceTile(Zombie zombie, int cell) {
    if (zombie.getIcedOnCell() == cell) {
      return;
    }
    zombie.setIcedOnCell(cell);
    if (!zombie.getActiveEffects().containsKey(StatusEffect.FROZEN)) {
      zombie.applyEffect(StatusEffect.FROZEN, ICE_TILE_FREEZE_TICKS);
    }
  }
  private void slideZombie(Zombie zombie, int laneShift) {
    if (zombie.getBehavior() instanceof model.game.zombie.behavior.DodoRiderZombieAction) {
      return;
    }
    int targetRow = zombie.getRow() + laneShift;
    if (targetRow < 0 || targetRow >= rows) {
      targetRow = zombie.getRow() - laneShift;
    }
    if (targetRow < 0 || targetRow >= rows || targetRow == zombie.getRow()) {
      return;
    }
    zombie.setRow(targetRow);
    System.out.printf("%s slipped on the ice to row %d!%n", zombie.getDisplayName(), targetRow + 1);
  }

  public void setZombiesResistIce(boolean resist) {
    this.zombiesResistIce = resist;
  }

  public boolean zombiesResistIce() {
    return zombiesResistIce;
  }

  private boolean zombiesResistIce;


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
  private void retireDepartedHypnotizedZombies() {
    for (Zombie zombie : zombies) {
      if (zombie.isHypnotized() && !zombie.isDead() && zombie.getX() > columns) {
        System.out.printf("%s marched off the lawn and left the battle.%n", zombie.getDisplayName());
        zombie.takeDamage(zombie.getMaxHealth(), true);
        zombie.markLootDropped();
      }
    }
  }
  private void handleGlowingZombieDrops() {
    for (Zombie zombie : zombies) {
      if (zombie.isDead() && zombie.isShiny() && !zombie.hasDroppedPlantFood()) {
        zombie.markPlantFoodDropped();
        if (gameState.addPlantFood()) {
          notify(String.format(
                  "The glowing zombie dropped a plant food; you have %d plant foods now.",
                  gameState.getPlantFoodCount()));
        }
      }
    }
  }
  private void handleDeathDrops() {
    for (Zombie zombie : zombies) {
      if (zombie.isDead() && !zombie.hasDroppedLoot()) {
        zombie.markLootDropped();
        if (zombie.getBehavior() != null) {
          zombie.getBehavior().onDeath(zombie, this);
        }
        if (zombie.getSpeed() > FAST_ZOMBIE_SPEED_THRESHOLD) {
          pendingFastZombieKills++;
        }
        pendingKillCount++;
        pendingKillDetails.add(new KillDetail(
                zombie.getRow(),
                Math.round(zombie.getX()),
                laneHasUnusedMower(zombie.getRow()),
                mowerVictims.contains(zombie)));
        lootDropper.rollFor(zombie);
      }
    }
  }
  private boolean laneHasUnusedMower(int row) {
    if (row < 0 || row >= lawnmowers.size()) {
      return false;
    }
    return lawnmowers.get(row).isAvailable();
  }
  public List<Reward> drainPendingRewards() {
    return lootDropper.drainPendingRewards();
  }
  public List<LootDropper.LootSpawn> drainPendingLootSpawns() {
    return lootDropper.drainPendingLootSpawns();
  }
  private void notify(String message) {
    // The terminal never drains, so the queue is capped rather than left to grow all match.
    if (pendingNotices.size() >= MAX_PENDING_NOTICES) {
      pendingNotices.remove(0);
    }
    pendingNotices.add(message);
    System.out.println(message);
  }
  public List<String> drainPendingNotices() {
    List<String> drained = new ArrayList<>(pendingNotices);
    pendingNotices.clear();
    drained.addAll(lootDropper.drainPendingNotices());
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
  public int drainPendingFastZombieKills() {
    int count = pendingFastZombieKills;
    pendingFastZombieKills = 0;
    return count;
  }
  public int drainPendingMultiKillShots() {
    int count = pendingMultiKillShots;
    pendingMultiKillShots = 0;
    return count;
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
        // فقط قبرهایی که «قابلیت نکرومنسی» دارند زامبی بیرون می‌دهند (طبق داک، همهٔ خانه‌ها این
        if (effect instanceof TombStoneEffect tombstone
                && tombstone.isActive()
                && tombstone.isNecromancy()) {
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
      if (other == zombie || other.isDead() || !other.occupiesRow(zombie.getRow())) {
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

  private void handleBouncingProjectile(Projectile p, ListIterator<Projectile> iterator) {
    if (p.getYCoordinate() < 0 || p.getYCoordinate() >= rows) {
      p.bounceVertically(rows);
    }
    if (p.getXCoordinate() < 0 || p.getXCoordinate() >= columns) {
      p.bounceHorizontally(columns);
    }
    if (p.isExpired()) {
      iterator.remove();
    }
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
          p = p.ignited(plantHere.isBlueFlame());
          iterator.set(p);
        }
      }
      if (isBlockedByTombstone(p) || breaksIceBlock(p)) {
        iterator.remove();
        continue;
      }
      reaimLob(p);
      boolean hitRegistered = false;
      for (Zombie zombie : zombies) {
        if (zombie.occupiesRow(p.getYCoordinate())
                && Math.abs(zombie.getX() - p.getXCoordinate()) < 0.5) {
          p.hitArea(zombies, zombie);
          if (zombiesResistIce) {
            zombie.extinguishFrozenStatus();
          }
          if (p.getKillCount() == 2) {
            pendingMultiKillShots++;
          }
          hitRegistered = true;
          break;
        }
      }
      if (p.isBouncing()) {
        handleBouncingProjectile(p, iterator);
        continue;
      }

      if ((hitRegistered && !p.isPiercing()) || p.getXCoordinate() > columns || p.getXCoordinate() < -1
              || p.getYCoordinate() < 0 || p.getYCoordinate() >= rows) {
        iterator.remove();
      }
    }
  }
  private void reaimLob(Projectile p) {
    if (!p.isLobbed() || p.isFromZombie()) {
      return;
    }
    int row = Math.round(p.getYCoordinate());
    double nearest = Double.MAX_VALUE;
    for (Zombie zombie : zombies) {
      if (zombie.isDead() || zombie.getRow() != row || zombie.getX() < p.getXCoordinate()) {
        continue;
      }
      nearest = Math.min(nearest, zombie.getX());
    }
    if (nearest != Double.MAX_VALUE && nearest > p.getLaunchX()) {
      p.aimedAt(nearest);
    }
  }

  private boolean isBlockedByTombstone(Projectile p) {
    // تیرهای کمانی (lobber) طبق داک از روی موانع رد می‌شوند
    if (p.isFromZombie() || p.isLobbed()) {
      return false;
    }
    int row = Math.round(p.getYCoordinate());
    int col = (int) Math.round(p.getXCoordinate());
    if (row < 0 || row >= rows || col < 0 || col >= columns) {
      return false;
    }
    TileEffect effect = tiles[row][col].getEffect();
    if (effect instanceof BarrelEffect barrel && barrel.isActive()) {
      barrel.takeDamage(p.getDamage());
      return true;
    }
    if (effect instanceof TombStoneEffect tombstone
            && tombstone.isActive()
            && tombstone.isBlocksShots()) {
      // طبق داک سنگ‌قبر ۷۰۰ جان دارد و «وقتی تیر می‌خورد آسیب می‌بیند» تا نابود شود؛ قبلا تیر را
      tombstone.takeDamage(p.getDamage());
      return true;
    }
    return false;
  }
  // طبق داک، یخِ روی گیاه باید با تیر گیاهان شکسته شود و تیر آتشین آن را فورا آب می‌کند
  private boolean breaksIceBlock(Projectile p) {
    if (p.isLobbed()) {
      return false;
    }
    Plant iced = getPlantAt(Math.round(p.getYCoordinate()), p.getXCoordinate());
    if (iced == null || iced.getIceHealth() <= 0) {
      return false;
    }
    if (p.getEffect() == Projectile.ProjectileEffect.FIRE) {
      iced.meltIce();
    } else {
      iced.damageIce(p.getDamage());
    }
    return true;
  }

  private void handleLawnmowers() {
    for (Lawnmower mower : lawnmowers) {
      if (mower.isTriggered()) {
        mowerVictims.addAll(mower.move(zombies));
        continue;
      }
      if (!mower.isActive()) {
        for (Zombie z : zombies) {
          if (!z.isBoss() && z.occupiesRow(mower.getRow()) && z.getX() <= -0.5) {
            playerLost = true;
            return;
          }
        }
        continue;
      }
      for (Zombie z : zombies) {
        // A charging Zomboss must not burn the row's mower on its way past.
        if (!z.isBoss() && z.occupiesRow(mower.getRow()) && z.getX() <= 0.0) {
          triggerLawnmowerRow(mower);
          break;
        }
      }
    }
  }

  private void triggerLawnmowerRow(Lawnmower mower) {
    int row = mower.getRow();
    mower.trigger();
    System.out.printf(
            "The lawn mower in the row %d is triggered and killed these zombies:%n", row + 1);
    for (Zombie z : zombies) {
      if (!z.isBoss() && z.occupiesRow(row) && !z.isDead()) {
        System.out.println("- " + z.getDisplayName());
      }
    }
  }
  private void cleanupEntities() {
    for (Zombie zombie : zombies) {
      if (zombie.isDead()) {
        System.out.printf(
                "Zombie of type %s is dead at (%d, %d).%n",
                zombie.getDisplayName(), Math.round(zombie.getX()) + 1, zombie.getRow() + 1);
      }
    }
    for (Plant plant : plants) {
      if (plant.isDead()) {
        pendingPlantsLostCount++;
        System.out.printf(
                "Plant %s at (%d, %d) is destroyed.%n",
                plant.getName(), plant.getCol() + 1, plant.getRow() + 1);
      }
    }
    plants.removeIf(Plant::isDead);
    zombies.removeIf(Zombie::isDead);
    mowerVictims.clear();
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
  public Plant getTopPlantAt(int row, double x) {
    Plant top = null;
    for (Plant p : plants) {
      if (p.getRow() == row && Math.abs(p.getCol() - x) < 0.5
              && (top == null || top.getShield() != p)) {
        top = p;
      }
    }
    return top;
  }
  public Plant getEdiblePlantAt(int row, double x, int currentTick) {
    Plant plant = getTopPlantAt(row, x);
    if (plant != null && plant.isDisabled(currentTick)) {
      return null;
    }
    return plant;
  }
  public Zombie getZombieAt(int row, double x) {
    for (Zombie z : zombies) {
      if (z.occupiesRow(row) && !z.isDead() && Math.abs(z.getX() - x) < 0.5) {
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
    registerZombieAsSeen(z);
  }

  private void registerZombieAsSeen(Zombie z) {
    if (z == null || z.getName() == null) {
      return;
    }
    model.account.User user = data.persistence.UserManager.getInstance().getCurrentUser();
    if (user != null) {
      user.unlockItem("zombie_" + z.getName().toLowerCase());
    }
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
      if (zombie.occupiesRow(row) && !zombie.isDead() && zombie.getX() >= plantX) {
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

  public Integer collectSunAt(int col, int row, int currentTick) {
    return sunManager.collectSunAt(col, row, this, currentTick);
  }

  public boolean wasLastSunCollectFast() {
    return sunManager.wasLastCollectFast();
  }

  public void applyAreaDamageToZombies(int centerCol, int centerRow, int radius, int damage) {
    applyAreaDamageToZombies(centerCol, centerRow, radius, damage, null);
  }

  /** {@code spared} walks away from its own blast, which is how the Prospector keeps rolling. */
  public void applyAreaDamageToZombies(int centerCol, int centerRow, int radius, int damage,
          Zombie spared) {
    for (Zombie z : zombies) {
      if (z == spared || z.isDead() || Math.abs(z.getX() - centerCol) > radius) {
        continue;
      }
      if (Math.abs(z.getRow() - centerRow) <= radius
              || Math.abs(z.getBottomRow() - centerRow) <= radius) {
        z.takeBlastDamage(damage);
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
