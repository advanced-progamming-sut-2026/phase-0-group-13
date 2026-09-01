package model.game.zombie.behavior;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.enums.PlantTag;
import model.enums.StatusEffect;
import model.enums.ZombieType;
import model.game.Board;
import model.game.BossHazard;
import model.game.TileEffects.FireEffect;
import model.game.TileEffects.IceTrailEffect;
import model.game.plant.Plant;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.ZombieTemplate;
import model.game.zombie.ZombieParts.ZombieTypeResolver;
import model.game.zombie.factory.ZombieFactory;

public class ZombossAction implements ZombieAction {

  public static final int ROW_SPAN = 2;

  /**
   * What the boss is doing, for the rig to match.
   *
   * <p>SUMMONING is its own pose rather than folded into ATTACKING because every chapter's rig
   * draws it differently -- Egypt opens a portal, the pirate has a spawn, the mammoth calls up a
   * glacier column -- and none of those look anything like firing a missile.
   */
  public enum Pose { IDLE, MOVING, ATTACKING, SUMMONING, STUNNED }

  private static final int ATTACK_POSE_TICKS = 12;

  /** Longer than the attack pose: the portal and spawn clips are wind-ups, not a single blow. */
  private static final int SUMMON_POSE_TICKS = 18;

  private static final int STUN_TICKS = 40;

  private static final int SUMMON_INTERVAL = 200;
  private static final int ATTACK_INTERVAL = 90;
  private static final int ULTIMATE_INTERVAL = 320;
  private static final int ROW_MOVE_INTERVAL = 260;

  private static final int FIRE_TICKS = 180;
  private static final int FREEZE_TICKS = 200;

  private static final int SUCK_TICKS = 25;
  private static final double SUCK_SPEED = 0.35;
  private static final double SUCK_REACH = 0.6;

  private static final double CHARGE_SPEED = 0.12;
  private static final double CHARGE_STOP_COLUMN = 0.5;
  private static final double CRUSH_REACH = 0.7;

  private static final int DESTROY_DAMAGE = 100000;

  /** How long a missile or a boulder is in the air, in ticks: long enough to be seen coming. */
  private static final int HAZARD_FLIGHT_TICKS = 14;
  private static final int SHARK_COUNT = 2;

  /** How many of the iced column's tiles get a zombie planted in them, at most one per row. */
  private static final int FROZEN_COLUMN_ZOMBIES = 3;

  /** Frostbite's own walkers, so what thaws out of the ice belongs to the chapter. */
  private static final ZombieType[] FROZEN_MINION_TYPES =
      {ZombieType.TROGLOBITE, ZombieType.HUNTER, ZombieType.CONEHEAD};
  private static final double SHARK_SPEED = 0.06;

  private final ZombieType chapter;
  private final ZombossHealth health;
  private final double eatingDamage;
  private final Random random = new Random();

  private int stunTicksLeft;
  private int segmentsCleared;

  private int lastSummonTick = -1;
  private int lastAttackTick = -1;
  private int lastUltimateTick = -1;
  private int lastRowMoveTick = -1;
  private boolean started;

  private boolean charging;
  /** The beach boss alternates its two attacks, so both are seen rather than one at random. */
  private boolean sharkTurn;
  private int suckTicksLeft;
  private int suckTopRow = -1;
  private double station = -1;
  private Pose pose = Pose.IDLE;
  private int attackPoseLeft;
  private int summonPoseLeft;

  public ZombossAction(ZombieType chapter, ZombossHealth health, double eatingDamage) {
    this.chapter = chapter;
    this.health = health;
    this.eatingDamage = eatingDamage;
  }

  public ZombossHealth getHealth() {
    return health;
  }

  public ZombieType getChapter() {
    return chapter;
  }

  public boolean isStunned() {
    return stunTicksLeft > 0;
  }

  public Pose getPose() {
    return pose;
  }

  public int getStunTicksLeft() {
    return stunTicksLeft;
  }

  @Override
  public void execute(Zombie zombie, Board board, int currentTick) {
    if (zombie.isDead()) {
      return;
    }
    if (station < 0) {
      station = Math.max(1, board.getColumns() - 2);
      zombie.setRow(Math.max(0, Math.min(zombie.getRow(), board.getRows() - ROW_SPAN)));
    }
    seedTimers(currentTick);
    checkSegments(zombie);

    if (stunTicksLeft > 0) {
      stunTicksLeft--;
      pose = Pose.STUNNED;
      zombie.setEating(false);
      return;
    }
    if (suckTicksLeft > 0) {
      suckTicksLeft--;
      pose = Pose.ATTACKING;
      dragRowsIn(zombie, board);
      return;
    }
    if (charging) {
      pose = Pose.MOVING;
      advanceCharge(zombie, board);
      return;
    }

    // The summon pose runs to its end before the boss goes back to firing: the portal and spawn
    // clips are wind-ups, and cutting one off after a tick shows nothing at all.
    if (summonPoseLeft > 0) {
      summonPoseLeft--;
      // Station-keeping first and the pose after it, not the other way round: crushOrHoldStation
      // picks a pose of its own every time it runs, so setting SUMMONING before calling it had the
      // summon wiped on the very next tick and the portal clip never got past its first frame.
      crushOrHoldStation(zombie, board, currentTick);
      pose = Pose.SUMMONING;
      return;
    }
    if (movesBetweenRows() && currentTick - lastRowMoveTick >= ROW_MOVE_INTERVAL) {
      moveToAnotherRow(zombie, board, currentTick);
    }
    if (summonsZombies() && currentTick - lastSummonTick >= SUMMON_INTERVAL) {
      summonMinion(zombie, board, currentTick);
      summonPoseLeft = SUMMON_POSE_TICKS;
      pose = Pose.SUMMONING;
      return;
    }
    if (currentTick - lastUltimateTick >= ULTIMATE_INTERVAL) {
      attackPoseLeft = ATTACK_POSE_TICKS;
      pose = Pose.ATTACKING;
      unleashUltimate(zombie, board, currentTick);
      return;
    }
    if (currentTick - lastAttackTick >= ATTACK_INTERVAL) {
      attackPoseLeft = ATTACK_POSE_TICKS;
      fireAtTheLawn(zombie, board, currentTick);
    }
    crushOrHoldStation(zombie, board, currentTick);
    if (attackPoseLeft > 0) {
      attackPoseLeft--;
      pose = Pose.ATTACKING;
    }
  }

  private void seedTimers(int currentTick) {
    if (started) {
      return;
    }
    started = true;
    lastSummonTick = currentTick;
    lastAttackTick = currentTick;
    lastRowMoveTick = currentTick;
    // Offset so the big attack is never the first thing the player sees.
    lastUltimateTick = currentTick + ATTACK_INTERVAL;
  }

  private void checkSegments(Zombie zombie) {
    int cleared = health.segmentsCleared(zombie.getCurrentHealth());
    if (cleared <= segmentsCleared) {
      return;
    }
    segmentsCleared = cleared;
    if (zombie.isDead()) {
      return;
    }
    stunTicksLeft = STUN_TICKS;
    charging = false;
    suckTicksLeft = 0;
    System.out.printf(
            "%s took a segment of damage and is stunned! %d of %d segments left.%n",
            zombie.getDisplayName(), health.segmentsLeft(zombie.getCurrentHealth()),
            ZombossHealth.SEGMENTS);
  }


  private boolean movesBetweenRows() {
    return chapter != ZombieType.ZOMBOSS_COWBOY;
  }

  private boolean summonsZombies() {
    return chapter != ZombieType.ZOMBOSS_COWBOY;
  }

  private void moveToAnotherRow(Zombie zombie, Board board, int currentTick) {
    lastRowMoveTick = currentTick;
    int topmost = board.getRows() - ROW_SPAN;
    if (topmost < 1) {
      return;
    }
    int target = random.nextInt(topmost + 1);
    if (target == zombie.getRow()) {
      target = (target + 1) % (topmost + 1);
    }
    zombie.setRow(target);
    System.out.printf("%s stomped across into rows %d and %d.%n",
            zombie.getDisplayName(), target + 1, target + ROW_SPAN);
  }

  private void summonMinion(Zombie zombie, Board board, int currentTick) {
    lastSummonTick = currentTick;
    if (GameDataManager.zombieRepository == null) {
      return;
    }
    String alias = aliasForAny(minionTypes());
    if (alias == null) {
      return;
    }
    int lane = board.getRows() > 0 ? random.nextInt(board.getRows()) : zombie.getRow();
    Zombie minion = new ZombieFactory(GameDataManager.zombieRepository)
            .createZombie(alias, lane, Math.min(board.getColumns(), zombie.getX()));
    if (minion != null) {
      board.spawnZombie(minion);
      System.out.printf("%s dropped a %s into row %d!%n",
              zombie.getDisplayName(), minion.getDisplayName(), lane + 1);
    }
  }

  /** داک: هر تیپی جز مَمِوت مجاز است. */
  private ZombieType[] minionTypes() {
    return switch (chapter) {
      case ZOMBOSS_DARK -> new ZombieType[] {ZombieType.IMP_DRAGON, ZombieType.JUGGLER};
      case ZOMBOSS_PIRATE -> new ZombieType[] {ZombieType.SNORKEL, ZombieType.OCTOPUS};
      default -> new ZombieType[] {ZombieType.IMP, ZombieType.CONEHEAD};
    };
  }

  private void crushOrHoldStation(Zombie zombie, Board board, int currentTick) {
    Plant underfoot = plantUnderneath(zombie, board, currentTick);
    if (underfoot != null) {
      zombie.setEating(true);
      pose = Pose.ATTACKING;
      if (currentTick % 10 == 0) {
        underfoot.takeDamage((int) eatingDamage);
      }
      return;
    }
    zombie.setEating(false);
    double step = Math.max(0.005, zombie.getSpeed());
    double gap = station - zombie.getX();
    if (Math.abs(gap) <= step) {
      zombie.setX(station);
      pose = Pose.IDLE;
      return;
    }
    zombie.moveTo(zombie.getX() + Math.signum(gap) * step);
    pose = Pose.MOVING;
  }

  private Plant plantUnderneath(Zombie zombie, Board board, int currentTick) {
    for (int row = zombie.getRow(); row <= zombie.getBottomRow(); row++) {
      Plant plant = board.getEdiblePlantAt(row, zombie.getX(), currentTick);
      if (plant != null && !plant.isDead()) {
        return plant;
      }
    }
    return null;
  }


  private void fireAtTheLawn(Zombie zombie, Board board, int currentTick) {
    lastAttackTick = currentTick;
    switch (chapter) {
      case ZOMBOSS_DARK -> breatheFireball(zombie, board);
      case ZOMBOSS_COWBOY -> fireIceMissile(zombie, board);
      case ZOMBOSS_PIRATE -> attackFromTheSea(zombie, board);
      default -> fireMissile(zombie, board);
    }
  }

  /**
   * Egypt's rocket, which is now fired rather than simply landed.
   *
   * <p>The plant used to die on the tick the boss chose it, so the whole attack was a line of
   * text. The missile is put in the air instead and does its damage -- the flattened tile and the
   * graves the blast throws up -- when it arrives, which is {@link BossHazard}'s job.
   */
  private void fireMissile(Zombie zombie, Board board) {
    int[] cell = randomCell(board);
    System.out.printf("%s fired a missile at (%d, %d)!%n",
            zombie.getDisplayName(), cell[1] + 1, cell[0] + 1);
    board.addBossHazard(BossHazard.missile(cell[0], cell[1], HAZARD_FLIGHT_TICKS));
  }

  private void breatheFireball(Zombie zombie, Board board) {
    int[] cell = randomCell(board);
    System.out.printf("%s breathed a fireball onto (%d, %d)!%n",
            zombie.getDisplayName(), cell[1] + 1, cell[0] + 1);
    destroyPlantAt(board, cell[0], cell[1]);
    board.placeTileEffect(cell[0], cell[1], new FireEffect(FIRE_TICKS));
    spawnAt(board, ZombieType.IMP_DRAGON, cell[0], cell[1]);
  }

  /** The mammoth's slingshot: the same flight as Egypt's rocket, with a colder landing. */
  private void fireIceMissile(Zombie zombie, Board board) {
    int[] cell = randomCell(board);
    System.out.printf("%s slung an ice boulder at (%d, %d)!%n",
            zombie.getDisplayName(), cell[1] + 1, cell[0] + 1);
    board.addBossHazard(BossHazard.iceBoulder(cell[0], cell[1], HAZARD_FLIGHT_TICKS));
  }

  /** The beach boss has two of these; alternating means neither goes unseen for a whole match. */
  private void attackFromTheSea(Zombie zombie, Board board) {
    if (sharkTurn) {
      releaseSharks(zombie, board);
    } else {
      sendLittleOctopus(zombie, board);
    }
    sharkTurn = !sharkTurn;
  }

  /**
   * Little sharks, let loose at the boss's end of the lawn to swim up their rows.
   *
   * <p>Unlike the octopus, which reaches out and eats a plant wherever it is, a shark has to get
   * there: it crosses the row a tile at a time and takes the first plant it runs into, so a row
   * the player has left empty costs them nothing.
   */
  private void releaseSharks(Zombie zombie, Board board) {
    if (board.getRows() <= 0) {
      return;
    }
    for (int i = 0; i < SHARK_COUNT; i++) {
      int row = random.nextInt(board.getRows());
      board.addBossHazard(BossHazard.shark(row, zombie.getX(), SHARK_SPEED));
    }
    System.out.printf("%s let %d little sharks loose on the lawn!%n",
            zombie.getDisplayName(), SHARK_COUNT);
  }

  private void sendLittleOctopus(Zombie zombie, Board board) {
    Plant victim = randomLivingPlant(board);
    if (victim == null) {
      return;
    }
    System.out.printf("A little octopus surfaced and ate the %s at (%d, %d)!%n",
            victim.getName(), victim.getCol() + 1, victim.getRow() + 1);
    victim.takeDamage(DESTROY_DAMAGE);
  }


  private void unleashUltimate(Zombie zombie, Board board, int currentTick) {
    lastUltimateTick = currentTick;
    switch (chapter) {
      case ZOMBOSS_DARK -> igniteOppositeRows(zombie, board);
      case ZOMBOSS_COWBOY -> deepFreeze(zombie, board, currentTick);
      case ZOMBOSS_PIRATE -> beginSuckingIn(zombie, board);
      default -> beginCharge(zombie);
    }
  }

  private void beginCharge(Zombie zombie) {
    charging = true;
    System.out.printf("%s is charging down rows %d and %d!%n",
            zombie.getDisplayName(), zombie.getRow() + 1, zombie.getBottomRow() + 1);
  }

  /**
   * The charge itself: fast, forward, and over anything in the way.
   *
   * <p>moveTo and not setX. setX drags previousX along with it, which is right for a boss being
   * put back on the far side of the lawn and wrong for one running across it: the renderer had
   * nothing to tween and the charge came out as ten 0.12-column jumps a second.
   */
  private void advanceCharge(Zombie zombie, Board board) {
    zombie.setEating(false);
    zombie.moveTo(zombie.getX() - CHARGE_SPEED);
    for (Plant plant : new ArrayList<>(board.getPlants())) {
      if (!plant.isDead()
              && plant.getRow() >= zombie.getRow()
              && plant.getRow() <= zombie.getBottomRow()
              && Math.abs(plant.getCol() - zombie.getX()) <= CRUSH_REACH) {
        plant.takeDamage(DESTROY_DAMAGE);
      }
    }
    trampleZombiesInTheWay(zombie, board);
    if (zombie.getX() <= CHARGE_STOP_COLUMN) {
      charging = false;
      zombie.setX(board.getColumns() - 1);
      System.out.printf("%s jumped back to the far side of the lawn.%n", zombie.getDisplayName());
    }
  }

  /** A charging boss goes through its own zombies as readily as through the plants. */
  private void trampleZombiesInTheWay(Zombie zombie, Board board) {
    for (Zombie other : new ArrayList<>(board.getZombies())) {
      if (other == zombie || other.isBoss() || other.isDead()) {
        continue;
      }
      boolean inTheWay = other.occupiesRow(zombie.getRow())
              || other.occupiesRow(zombie.getBottomRow());
      if (inTheWay && Math.abs(other.getX() - zombie.getX()) <= CRUSH_REACH) {
        other.takeDamage(DESTROY_DAMAGE, true);
      }
    }
  }

  private void igniteOppositeRows(Zombie zombie, Board board) {
    int top = oppositeTopRow(zombie, board);
    for (int row = top; row < top + ROW_SPAN && row < board.getRows(); row++) {
      for (int col = 0; col < board.getColumns(); col++) {
        destroyPlantAt(board, row, col);
        board.placeTileEffect(row, col, new FireEffect(FIRE_TICKS));
      }
    }
    System.out.printf("%s set rows %d and %d ablaze!%n",
            zombie.getDisplayName(), top + 1, Math.min(board.getRows(), top + ROW_SPAN));
  }

  private void deepFreeze(Zombie zombie, Board board, int currentTick) {
    int rows = board.getRows();
    int first = random.nextInt(rows);
    int second = rows > 1 ? (first + 1 + random.nextInt(rows - 1)) % rows : first;
    freezePlantsInRow(board, first, currentTick);
    freezePlantsInRow(board, second, currentTick);
    System.out.printf("%s sent an ice wind tearing down rows %d and %d!%n",
            zombie.getDisplayName(), first + 1, second + 1);

    int column = random.nextInt(board.getColumns());
    for (int row = 0; row < rows; row++) {
      board.placeTileEffect(row, column, new IceTrailEffect(FREEZE_TICKS, 0.0, true));
    }
    for (Zombie other : board.getZombies()) {
      if (other != zombie && !other.isDead() && Math.round(other.getX()) == column) {
        other.applyEffect(StatusEffect.FROZEN, FREEZE_TICKS);
      }
    }
    plantFrozenZombiesInColumn(zombie, board, column);
    System.out.printf("Column %d froze solid, zombies and all.%n", column + 1);

    // The column freeze is the mammoth putting zombies on the lawn, so it holds the summoning pose
    // rather than the attacking one: its rig's glacier_column clips are exactly this move, and
    // under ATTACKING the boss played its slingshot -- the missile throw -- while icing a column.
    // attackPoseLeft is cleared because unleashUltimate arms it before dispatching here, and
    // otherwise it would take the pose straight back the tick the summon ran out.
    attackPoseLeft = 0;
    summonPoseLeft = SUMMON_POSE_TICKS;
    pose = Pose.SUMMONING;
  }

  /**
   * The other half of the mammoth's column freeze: it plants frozen zombies in the tiles it just
   * iced over.
   *
   * <p>The doc's move is "freezes one column at random and plants frozen zombies in that column's
   * tiles"; only the icing was happening, so the column went cold and stayed empty and the move
   * cost the player nothing once it thawed. These arrive frozen for as long as the ice lasts,
   * which is the same deal Frostbite's own frozen zombies get, and thaw into ordinary walkers.
   *
   * <p>Not a contradiction of the mammoth summoning no zombies: that rule is about the roaming
   * minions the other three bosses drop, and these are part of this one's own attack.
   */
  private void plantFrozenZombiesInColumn(Zombie zombie, Board board, int column) {
    if (GameDataManager.zombieRepository == null || board.getRows() <= 0) {
      return;
    }
    String alias = aliasForAny(FROZEN_MINION_TYPES);
    if (alias == null) {
      return;
    }
    int planted = 0;
    for (int row = 0; row < board.getRows() && planted < FROZEN_COLUMN_ZOMBIES; row++) {
      Zombie frozen = new ZombieFactory(GameDataManager.zombieRepository)
              .createZombie(alias, row, column);
      if (frozen == null) {
        continue;
      }
      frozen.applyEffect(StatusEffect.FROZEN, FREEZE_TICKS);
      board.spawnZombie(frozen);
      planted++;
    }
    if (planted > 0) {
      System.out.printf("%s planted %d frozen zombies down column %d.%n",
              zombie.getDisplayName(), planted, column + 1);
    }
  }

  private void freezePlantsInRow(Board board, int row, int currentTick) {
    for (Plant plant : board.getPlants()) {
      if (!plant.isDead() && plant.getRow() == row && !plant.getTags().contains(PlantTag.FIRE)) {
        plant.freeze(currentTick, FREEZE_TICKS);
      }
    }
  }

  private void beginSuckingIn(Zombie zombie, Board board) {
    suckTopRow = oppositeTopRow(zombie, board);
    suckTicksLeft = SUCK_TICKS;
    System.out.printf("%s opened his maw and started dragging rows %d and %d in!%n",
            zombie.getDisplayName(), suckTopRow + 1,
            Math.min(board.getRows(), suckTopRow + ROW_SPAN));
  }

  /**
   * The turbine running: everything in those two rows comes towards the boss.
   *
   * <p>The rows used to be flattened on the tick the turbine switched on, so nothing was ever seen
   * being pulled anywhere -- the plants were already gone before the suction clip had a frame on
   * screen. Plants cannot be moved off their tile, so instead the row is taken from the boss's end
   * inwards, one plant a tick, while its own zombies are dragged bodily in and eaten.
   */
  private void dragRowsIn(Zombie zombie, Board board) {
    zombie.setEating(false);
    for (int row = suckTopRow; row < suckTopRow + ROW_SPAN && row < board.getRows(); row++) {
      Plant nearest = plantNearestTheBoss(board, row, zombie.getX());
      if (nearest != null) {
        nearest.takeDamage(DESTROY_DAMAGE);
      }
    }
    for (Zombie other : board.getZombies()) {
      if (other == zombie || other.isBoss() || other.isDead()
              || other.getRow() < suckTopRow || other.getRow() > suckTopRow + ROW_SPAN - 1) {
        continue;
      }
      double gap = zombie.getX() - other.getX();
      if (Math.abs(gap) <= SUCK_REACH) {
        other.takeDamage(DESTROY_DAMAGE, true);
        continue;
      }
      other.moveTo(other.getX() + Math.signum(gap) * SUCK_SPEED);
    }
  }

  /** The plant in this row closest to the turbine, which is the next one to go into it. */
  private static Plant plantNearestTheBoss(Board board, int row, double bossColumn) {
    Plant nearest = null;
    for (Plant plant : board.getPlants()) {
      if (plant.isDead() || plant.getRow() != row || plant.getCol() > bossColumn) {
        continue;
      }
      if (nearest == null || plant.getCol() > nearest.getCol()) {
        nearest = plant;
      }
    }
    return nearest;
  }

  private static int oppositeTopRow(Zombie zombie, Board board) {
    int rows = board.getRows();
    return zombie.getRow() < rows / 2 ? Math.max(0, rows - ROW_SPAN) : 0;
  }


  private int[] randomCell(Board board) {
    return new int[] {random.nextInt(board.getRows()), random.nextInt(board.getColumns())};
  }

  private boolean destroyPlantAt(Board board, int row, int col) {
    Plant plant = board.getPlantAt(row, col);
    if (plant == null || plant.isDead()) {
      return false;
    }
    plant.takeDamage(DESTROY_DAMAGE);
    return true;
  }

  private Plant randomLivingPlant(Board board) {
    List<Plant> alive = new ArrayList<>();
    for (Plant plant : board.getPlants()) {
      if (!plant.isDead()) {
        alive.add(plant);
      }
    }
    return alive.isEmpty() ? null : alive.get(random.nextInt(alive.size()));
  }

  private void spawnAt(Board board, ZombieType type, int row, int col) {
    if (GameDataManager.zombieRepository == null) {
      return;
    }
    String alias = aliasForAny(new ZombieType[] {type});
    if (alias == null) {
      return;
    }
    Zombie spawned = new ZombieFactory(GameDataManager.zombieRepository)
            .createZombie(alias, row, col);
    if (spawned != null) {
      board.spawnZombie(spawned);
      System.out.printf("A %s clambered out of the flames at (%d, %d)!%n",
              spawned.getDisplayName(), col + 1, row + 1);
    }
  }

  private String aliasForAny(ZombieType[] types) {
    List<String> found = new ArrayList<>();
    for (ZombieType type : types) {
      String alias = aliasFor(type);
      if (alias != null) {
        found.add(alias);
      }
    }
    return found.isEmpty() ? null : found.get(random.nextInt(found.size()));
  }

  private static String aliasFor(ZombieType type) {
    // Guarded here as well as at each call site: every caller has to check anyway, and one that
    // forgets gets a null alias and a summon that does not happen rather than an NPE mid-tick.
    if (GameDataManager.zombieRepository == null) {
      return null;
    }
    for (ZombieTemplate template : GameDataManager.zombieRepository.getAll()) {
      String alias = template.getName();
      if (alias != null && alias.startsWith("Zombie")
              && ZombieTypeResolver.resolve(template) == type) {
        return alias;
      }
    }
    return null;
  }
}
