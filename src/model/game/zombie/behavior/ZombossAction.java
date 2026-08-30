package model.game.zombie.behavior;

import data.GameDataManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.enums.PlantTag;
import model.enums.StatusEffect;
import model.enums.ZombieType;
import model.game.Board;
import model.game.TileEffects.FireEffect;
import model.game.TileEffects.IceTrailEffect;
import model.game.TileEffects.TombStoneEffect;
import model.game.plant.Plant;
import model.game.zombie.Zombie;
import model.game.zombie.ZombieParts.ZombieTemplate;
import model.game.zombie.ZombieParts.ZombieTypeResolver;
import model.game.zombie.factory.ZombieFactory;

/**
 * دکتر زامباس، باسِ آخرِ هر فصل.
 *
 * <p>Holds the right-hand columns rather than walking to the house -- the pressure in a boss stage
 * comes from what it throws at the lawn, not from the robot reaching the porch. On top of the parts
 * every chapter shares (three health segments with a stun between them, moving between lanes,
 * dropping the odd zombie) each chapter gets the two attacks the doc names for it: a small one it
 * fires often and the big one it saves.
 */
public class ZombossAction implements ZombieAction {

  /** Zomboss stands in two lanes, so plants in either of them can shoot it. */
  public static final int ROW_SPAN = 2;

  /** Long enough to be a real opening; the sheet's own StunTime is 3-4 seconds. */
  private static final int STUN_TICKS = 40;

  private static final int SUMMON_INTERVAL = 200;
  private static final int ATTACK_INTERVAL = 90;
  private static final int ULTIMATE_INTERVAL = 320;
  private static final int ROW_MOVE_INTERVAL = 260;

  private static final int FIRE_TICKS = 180;
  private static final int FREEZE_TICKS = 200;
  private static final int GRAVE_HEALTH = 500;
  private static final int MISSILE_GRAVES = 2;

  private static final int SUCK_TICKS = 25;
  private static final double SUCK_SPEED = 0.35;
  private static final double SUCK_REACH = 0.6;

  private static final double CHARGE_SPEED = 0.12;
  private static final double CHARGE_STOP_COLUMN = 0.5;
  private static final double CRUSH_REACH = 0.7;

  /** More than anything on the lawn has, so destroyed means destroyed. */
  private static final int DESTROY_DAMAGE = 100000;

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
  private int suckTicksLeft;
  private int suckTopRow = -1;
  private double station = -1;

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

  /** Stunned means a free hit: it neither moves nor attacks. */
  public boolean isStunned() {
    return stunTicksLeft > 0;
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
      // Both its lanes have to be on the board, wherever the wave put it.
      zombie.setRow(Math.max(0, Math.min(zombie.getRow(), board.getRows() - ROW_SPAN)));
    }
    seedTimers(currentTick);
    checkSegments(zombie);

    if (stunTicksLeft > 0) {
      stunTicksLeft--;
      zombie.setEating(false);
      return;
    }
    if (suckTicksLeft > 0) {
      suckTicksLeft--;
      dragRowsIn(zombie, board);
      return;
    }
    if (charging) {
      advanceCharge(zombie, board);
      return;
    }

    if (movesBetweenRows() && currentTick - lastRowMoveTick >= ROW_MOVE_INTERVAL) {
      moveToAnotherRow(zombie, board, currentTick);
    }
    if (summonsZombies() && currentTick - lastSummonTick >= SUMMON_INTERVAL) {
      summonMinion(zombie, board, currentTick);
    }
    if (currentTick - lastUltimateTick >= ULTIMATE_INTERVAL) {
      unleashUltimate(zombie, board, currentTick);
      return;
    }
    if (currentTick - lastAttackTick >= ATTACK_INTERVAL) {
      fireAtTheLawn(zombie, board, currentTick);
    }
    crushOrHoldStation(zombie, board, currentTick);
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

  /** A cleared segment is a stun, which is the whole rhythm of the fight. */
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

  // ---- shared behaviour ----------------------------------------------------

  /** فقط مَمِوتِ غارهای یخی بین ردیف‌ها جابه‌جا نمی‌شود و زامبی هم احضار نمی‌کند. */
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

  /** Squashes whatever is under it, otherwise walks back to the column it holds. */
  private void crushOrHoldStation(Zombie zombie, Board board, int currentTick) {
    Plant underfoot = plantUnderneath(zombie, board, currentTick);
    if (underfoot != null) {
      zombie.setEating(true);
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
      return;
    }
    zombie.setX(zombie.getX() + Math.signum(gap) * step);
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

  // ---- the small attack, one per chapter -----------------------------------

  private void fireAtTheLawn(Zombie zombie, Board board, int currentTick) {
    lastAttackTick = currentTick;
    switch (chapter) {
      case ZOMBOSS_DARK -> breatheFireball(zombie, board);
      case ZOMBOSS_COWBOY -> fireIceMissile(zombie, board);
      case ZOMBOSS_PIRATE -> sendLittleOctopus(zombie, board);
      default -> fireMissile(zombie, board);
    }
  }

  /** مصر: موشک به یک خانهٔ تصادفی؛ گیاه نابود می‌شود و دو قبر تازه بالا می‌آید. */
  private void fireMissile(Zombie zombie, Board board) {
    int[] cell = randomCell(board);
    System.out.printf("%s fired a missile at (%d, %d)!%n",
            zombie.getDisplayName(), cell[1] + 1, cell[0] + 1);
    destroyPlantAt(board, cell[0], cell[1]);
    for (int i = 0; i < MISSILE_GRAVES; i++) {
      int[] grave = randomCell(board);
      if (board.getPlantAt(grave[0], grave[1]) != null) {
        continue;
      }
      board.placeTileEffect(grave[0], grave[1], new TombStoneEffect(GRAVE_HEALTH, true));
      System.out.printf("The blast threw up a grave at (%d, %d).%n", grave[1] + 1, grave[0] + 1);
    }
  }

  /** دوران تاریکی: گلولهٔ آتش؛ خانه آتش می‌گیرد و یک ایمپ‌اژدها از دلش بیرون می‌آید. */
  private void breatheFireball(Zombie zombie, Board board) {
    int[] cell = randomCell(board);
    System.out.printf("%s breathed a fireball onto (%d, %d)!%n",
            zombie.getDisplayName(), cell[1] + 1, cell[0] + 1);
    destroyPlantAt(board, cell[0], cell[1]);
    board.placeTileEffect(cell[0], cell[1], new FireEffect(FIRE_TICKS));
    spawnAt(board, ZombieType.IMP_DRAGON, cell[0], cell[1]);
  }

  /** غارهای یخی: موشک یخی، فقط گیاه همان خانه را می‌برد. */
  private void fireIceMissile(Zombie zombie, Board board) {
    int[] cell = randomCell(board);
    System.out.printf("%s fired an ice missile at (%d, %d)!%n",
            zombie.getDisplayName(), cell[1] + 1, cell[0] + 1);
    destroyPlantAt(board, cell[0], cell[1]);
  }

  /** ساحل: یک اختاپوس کوچک از زیر آب بالا می‌آید و یک گیاه را می‌خورد. */
  private void sendLittleOctopus(Zombie zombie, Board board) {
    Plant victim = randomLivingPlant(board);
    if (victim == null) {
      return;
    }
    System.out.printf("A little octopus surfaced and ate the %s at (%d, %d)!%n",
            victim.getName(), victim.getCol() + 1, victim.getRow() + 1);
    victim.takeDamage(DESTROY_DAMAGE);
  }

  // ---- the big attack, one per chapter -------------------------------------

  private void unleashUltimate(Zombie zombie, Board board, int currentTick) {
    lastUltimateTick = currentTick;
    switch (chapter) {
      case ZOMBOSS_DARK -> igniteOppositeRows(zombie, board);
      case ZOMBOSS_COWBOY -> deepFreeze(zombie, board, currentTick);
      case ZOMBOSS_PIRATE -> beginSuckingIn(zombie, board);
      default -> beginCharge(zombie);
    }
  }

  /** مصر: یورش به جلو و نابودی همهٔ گیاهانِ دو ردیف خودش. */
  private void beginCharge(Zombie zombie) {
    charging = true;
    System.out.printf("%s is charging down rows %d and %d!%n",
            zombie.getDisplayName(), zombie.getRow() + 1, zombie.getBottomRow() + 1);
  }

  private void advanceCharge(Zombie zombie, Board board) {
    zombie.setEating(false);
    zombie.setX(zombie.getX() - CHARGE_SPEED);
    for (Plant plant : new ArrayList<>(board.getPlants())) {
      if (!plant.isDead()
              && plant.getRow() >= zombie.getRow()
              && plant.getRow() <= zombie.getBottomRow()
              && Math.abs(plant.getCol() - zombie.getX()) <= CRUSH_REACH) {
        plant.takeDamage(DESTROY_DAMAGE);
      }
    }
    if (zombie.getX() <= CHARGE_STOP_COLUMN) {
      charging = false;
      zombie.setX(board.getColumns() - 1);
      System.out.printf("%s jumped back to the far side of the lawn.%n", zombie.getDisplayName());
    }
  }

  /** دوران تاریکی: دو ردیفِ روبه‌رو را آتش می‌زند؛ همهٔ گیاهانشان می‌سوزند. */
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

  /** غارهای یخی: باد یخی روی دو ردیف تصادفی، به‌علاوهٔ یک ستون که کامل یخ می‌زند. */
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
    System.out.printf("Column %d froze solid, zombies and all.%n", column + 1);
  }

  private void freezePlantsInRow(Board board, int row, int currentTick) {
    for (Plant plant : board.getPlants()) {
      if (!plant.isDead() && plant.getRow() == row && !plant.getTags().contains(PlantTag.FIRE)) {
        plant.freeze(currentTick, FREEZE_TICKS);
      }
    }
  }

  /** ساحل: گیاهان و زامبی‌های دو ردیفِ روبه‌رو را به سمت دهانش می‌کشد. */
  private void beginSuckingIn(Zombie zombie, Board board) {
    suckTopRow = oppositeTopRow(zombie, board);
    suckTicksLeft = SUCK_TICKS;
    System.out.printf("%s opened his maw and started dragging rows %d and %d in!%n",
            zombie.getDisplayName(), suckTopRow + 1,
            Math.min(board.getRows(), suckTopRow + ROW_SPAN));
    for (int row = suckTopRow; row < suckTopRow + ROW_SPAN && row < board.getRows(); row++) {
      for (int col = 0; col < board.getColumns(); col++) {
        destroyPlantAt(board, row, col);
      }
    }
  }

  private void dragRowsIn(Zombie zombie, Board board) {
    zombie.setEating(false);
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
      other.setX(other.getX() + Math.signum(gap) * SUCK_SPEED);
    }
  }

  /** دو ردیفِ آن‌طرفِ زمین، طوری که با ردیف‌های خود باس همپوشانی نداشته باشد. */
  private static int oppositeTopRow(Zombie zombie, Board board) {
    int rows = board.getRows();
    return zombie.getRow() < rows / 2 ? Math.max(0, rows - ROW_SPAN) : 0;
  }

  // ---- helpers -------------------------------------------------------------

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

  /** The sheet standing for one of these types, picked at random among the ones that exist. */
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
