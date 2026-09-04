package view.gdx.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.enums.MiniGameType;
import model.game.minigame.arcade.IZombieEngine;
import model.game.minigame.arcade.IZombieMatch.PlantView;
import model.game.minigame.arcade.IZombieMatch.ShotView;
import model.game.minigame.arcade.IZombieMatch.Snapshot;
import model.game.minigame.arcade.IZombieMatch.ZombieView;
import view.gdx.core.PvzGdxGame;
import view.gdx.render.ArcadeRenderer;

public abstract class SnapshotIZombieScreen extends ArcadeBoardScreen {

  protected static final float BRAIN_ROW_FILL = 0.42f;
  private static final float PEA_ROW_FILL = 0.22f;
  protected static final Color READY = new Color(1f, 1f, 1f, 1f);
  protected static final Color UNAVAILABLE = new Color(0.45f, 0.45f, 0.5f, 1f);
  private static final Color EATEN_BRAIN = new Color(0.35f, 0.35f, 0.38f, 0.55f);

  private final Map<Integer, Object> animationKeys = new HashMap<>();

  protected SnapshotIZombieScreen(PvzGdxGame game, MiniGameType type, int level) {
    super(game, type, level);
  }

  protected abstract Snapshot currentSnapshot();

  @Override
  protected String seasonKey() {
    return "dark";
  }

  @Override
  protected void drawWorld(float delta) {
    Snapshot state = currentSnapshot();
    if (state == null) {
      return;
    }
    Batch batch = context().getBatch();
    for (int row = 0; row < ROWS && row < state.brains().length; row++) {
      if (state.brains()[row]) {
        art.drawBesideLane(batch, art.icon("brain"), row, BRAIN_ROW_FILL, LANE_PROP_GAP);
      }
    }
    float alpha = context().getTickAlpha();
    for (PlantView plant : state.plants()) {
      art.drawPlant(batch, keyFor(plant.id()), plant.name(), plant.col(), plant.row(),
          plant.ticksToShot(), alpha);
    }
    for (ZombieView zombie : state.zombies()) {
      art.drawZombie(batch, keyFor(zombie.id()), zombie.type(),
          Math.max(0.0, zombie.column()), zombie.row(), zombie.eating());
    }
    // Drawn between the tile the pea was on last tick and the one it is on now, or a shot that
    // crosses half a tile per tick would hop across the lawn in five jumps.
    TextureRegion pea = art.icon("pea");
    for (ShotView shot : state.shots() == null ? List.<ShotView>of() : state.shots()) {
      art.drawProp(batch, pea, shot.column() - IZombieEngine.SHOT_SPEED * (1f - alpha), shot.row(),
          PEA_ROW_FILL);
    }
  }

  protected final Object keyFor(int entityId) {
    return animationKeys.computeIfAbsent(entityId, id -> new Object());
  }

  @Override
  protected void drawOverlays(ShapeRenderer shapes) {
    drawRedLine(shapes, IZombieEngine.RED_LINE_COLUMN, true);
    Snapshot state = currentSnapshot();
    if (state == null) {
      return;
    }
    for (int row = 0; row < ROWS && row < state.brains().length; row++) {
      if (!state.brains()[row]) {
        shapes.setColor(EATEN_BRAIN);
        shapes.rect(geometry.columnToX(0) - LANE_PROP_GAP - 28f,
            geometry.rowCentreY(row) - 5f, 26f, 10f);
      }
    }
    for (PlantView plant : state.plants()) {
      art.healthBar(shapes, plant.col(), plant.row(),
          plant.health() / (float) plant.maxHealth(), 0.8f);
    }
    for (ZombieView zombie : state.zombies()) {
      art.healthBar(shapes, Math.max(0.0, zombie.column()), zombie.row(),
          zombie.health() / (float) zombie.maxHealth(), 0.86f);
    }
  }

  @Override
  protected void drawOutlines(ShapeRenderer shapes) {
    Snapshot state = currentSnapshot();
    if (state == null) {
      return;
    }
    for (ZombieView zombie : state.zombies()) {
      if (ArcadeRenderer.lookOf(zombie.type()) == null) {
        art.outline(shapes, Math.max(0.0, zombie.column()), zombie.row());
      }
    }
  }

  protected static String clock(int ticksRemaining) {
    int seconds = ticksRemaining / IZombieEngine.TICKS_PER_SECOND;
    return String.format("%d:%02d", seconds / 60, seconds % 60);
  }
}
