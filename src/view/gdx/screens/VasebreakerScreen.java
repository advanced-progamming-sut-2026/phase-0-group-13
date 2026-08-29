package view.gdx.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import java.util.ArrayList;
import java.util.List;
import model.enums.MiniGameType;
import model.game.minigame.arcade.VasebreakerEngine;
import model.game.minigame.arcade.VasebreakerEngine.ArcadePlant;
import model.game.minigame.arcade.VasebreakerEngine.ArcadeZombie;
import model.game.minigame.arcade.VasebreakerEngine.PendingSeed;
import model.game.minigame.arcade.VasebreakerEngine.VaseContent;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.HudArt;
import view.gdx.ui.SeedCard;

/**
 * Vase Breaker: a lawn of vases, and whatever is inside them.
 *
 * <p>All of it is {@link VasebreakerEngine}'s -- which tile holds which vase, what comes out, how
 * long a dropped packet lasts, where the zombies are and whether the board is clear. This asks
 * and draws.
 *
 * <p>The three vases are the three the Phase One board prints as V? / VG / VX, in the world's own
 * art: the brown one wears a question mark and could be anything, the green one holds a plant, and
 * the purple one with the face is the gargantuar. A smashed vase leaves the world's burst behind
 * for a moment so the click reads as a hit, then the tile is free to plant on.
 */
public final class VasebreakerScreen extends ArcadeBoardScreen {

  private static final float FLASH_SECONDS = 0.35f;
  private static final float VASE_ROW_FILL = 0.82f;
  private static final float PACKET_ROW_FILL = 0.34f;
  private static final Color HELD = new Color(1f, 1f, 1f, 1f);
  private static final Color WILTING = new Color(1f, 0.72f, 0.45f, 1f);

  private final VasebreakerEngine engine;
  private final HudArt hudArt = new HudArt();
  private final float[][] flash = new float[ROWS][COLUMNS];
  private final List<SeedCard> cards = new ArrayList<>();

  private String heldSeed;
  private String shownSeeds = "";

  public VasebreakerScreen(PvzGdxGame game, int level) {
    super(game, MiniGameType.VASEBREAKER, level);
    this.engine = new VasebreakerEngine(level);
  }

  @Override
  protected String seasonKey() {
    return "dark";
  }

  @Override
  protected String title() {
    return "Vase Breaker  -  level " + getLevel();
  }

  @Override
  protected String statusLine() {
    return "vases left " + vasesLeft() + "   -   zombies " + engine.getZombies().size()
        + (heldSeed == null
            ? "   -   click a vase to break it"
            : "   -   holding " + heldSeed + ", click a clear tile");
  }

  private int vasesLeft() {
    int left = 0;
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        if (engine.getVaseGrid()[row][col] != VaseContent.NONE
            && !engine.getSmashedGrid()[row][col]) {
          left++;
        }
      }
    }
    return left;
  }

  /** The doc's "separate area": every packet on the ground, ready to be picked up. */
  @Override
  protected void buildPicker(Table picker, Skin skin) {
    cards.clear();
    picker.add(new Label("plants from vases", skin, "secondary")).padRight(10f);
    if (engine.getPendingSeeds().isEmpty()) {
      picker.add(new Label("nothing yet - break a green vase", skin, "secondary"));
      return;
    }
    for (PendingSeed seed : engine.getPendingSeeds()) {
      String plant = seed.getPlantName();
      SeedCard card = new SeedCard(skin, SeedCard.Size.COMPACT, plant, plant,
          art.plantPortrait(plant), hudArt, this::hold);
      card.setSelected(plant.equalsIgnoreCase(heldSeed));
      picker.add(card).width(110f).height(116f).padRight(4f);
      cards.add(card);
    }
    updateCards();
  }

  @Override
  protected void refreshPicker() {
    String signature = seedSignature();
    if (!signature.equals(shownSeeds)) {
      shownSeeds = signature;
      rebuildPicker();
      return;
    }
    updateCards();
  }

  private String seedSignature() {
    StringBuilder key = new StringBuilder();
    for (PendingSeed seed : engine.getPendingSeeds()) {
      key.append(seed.getPlantName()).append('@').append(seed.getRow()).append(',')
          .append(seed.getCol()).append(';');
    }
    return key.toString();
  }

  /** Only the countdown changes between rebuilds, so the cards are repainted rather than rebuilt. */
  private void updateCards() {
    List<PendingSeed> seeds = engine.getPendingSeeds();
    for (int i = 0; i < cards.size() && i < seeds.size(); i++) {
      int secondsLeft = Math.max(0, seeds.get(i).getTicksLeft() / 10);
      cards.get(i).setStatus(secondsLeft + "s left");
      cards.get(i).setTint(secondsLeft <= 3 ? WILTING : HELD);
    }
  }

  private void hold(String plant) {
    heldSeed = plant.equalsIgnoreCase(heldSeed) ? null : plant;
    rebuildPicker();
  }

  @Override
  protected boolean clearSelection() {
    if (heldSeed == null) {
      return false;
    }
    heldSeed = null;
    rebuildPicker();
    return true;
  }

  @Override
  protected void tickEngine() {
    engine.tick();
    // A packet the player was holding can wilt while it is held; drop it rather than let the
    // next click fail with "no fresh seed packet available".
    if (heldSeed != null && !isStillOffered(heldSeed)) {
      heldSeed = null;
    }
  }

  private boolean isStillOffered(String plant) {
    for (PendingSeed seed : engine.getPendingSeeds()) {
      if (seed.getPlantName().equalsIgnoreCase(plant)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected String onCellClicked(int row, int column) {
    if (engine.getVaseGrid()[row][column] != VaseContent.NONE
        && !engine.getSmashedGrid()[row][column]) {
      String result = engine.smash(row, column);
      if (!result.startsWith("error:")) {
        flash[row][column] = FLASH_SECONDS;
      }
      return result;
    }
    if (heldSeed == null) {
      return engine.getPendingSeeds().isEmpty()
          ? null
          : "pick a plant from the tray above, then click a clear tile";
    }
    String plant = heldSeed;
    heldSeed = null;
    return engine.plantSeed(plant, row, column);
  }

  @Override
  protected void drawWorld(float delta) {
    Batch batch = context().getBatch();
    for (ArcadePlant plant : engine.getPlants()) {
      art.drawPlant(batch, plant, plant.getName(), plant.getCol(), plant.getRow());
    }
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        drawTile(batch, row, col, delta);
      }
    }
    for (PendingSeed seed : engine.getPendingSeeds()) {
      art.drawProp(batch, art.plantPortrait(seed.getPlantName()), seed.getCol(), seed.getRow(),
          PACKET_ROW_FILL);
    }
    for (ArcadeZombie zombie : engine.getZombies()) {
      if (!zombie.isDead()) {
        art.drawZombie(batch, zombie, zombie.isGargantuar() ? "gargantuar" : "zombie",
            Math.max(0.0, zombie.getExactColumn()), zombie.getRow(), false);
      }
    }
  }

  private void drawTile(Batch batch, int row, int col, float delta) {
    if (flash[row][col] > 0f) {
      flash[row][col] -= delta;
      TextureRegion burst = art.icon("vasesmash");
      if (burst != null) {
        batch.setColor(1f, 1f, 1f, Math.max(0f, flash[row][col] / FLASH_SECONDS));
        art.drawProp(batch, burst, col, row, VASE_ROW_FILL);
        batch.setColor(Color.WHITE);
      }
    }
    VaseContent content = engine.getVaseGrid()[row][col];
    if (content == VaseContent.NONE || engine.getSmashedGrid()[row][col]) {
      return;
    }
    art.drawProp(batch, art.icon(vaseIcon(content)), col, row, VASE_ROW_FILL);
  }

  /** Phase One's own three kinds: unknown, plant, gargantuar. */
  private static String vaseIcon(VaseContent content) {
    return switch (content) {
      case PLANT_VASE -> "vaseplant";
      case GARGANTUAR_VASE -> "vasegargantuar";
      default -> "vaseunknown";
    };
  }

  @Override
  protected void drawOverlays(ShapeRenderer shapes) {
    for (ArcadeZombie zombie : engine.getZombies()) {
      if (!zombie.isDead()) {
        art.healthBar(shapes, Math.max(0.0, zombie.getExactColumn()), zombie.getRow(),
            zombie.getHealth() / (float) zombie.getMaxHealth(), 0.86f);
      }
    }
  }

  @Override
  protected boolean engineFinished() {
    return engine.isFinished();
  }

  @Override
  protected boolean engineWon() {
    return engine.isWon();
  }

  @Override
  protected String outcomeWon() {
    return "Every vase is broken and the lawn is clear!";
  }

  @Override
  public void dispose() {
    hudArt.dispose();
    super.dispose();
  }
}
