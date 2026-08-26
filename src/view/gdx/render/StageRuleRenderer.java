package view.gdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import model.core.GameManager;
import model.environment.AncientEgyptSeason;
import model.environment.BigWaveBeachSeason;
import model.environment.FrostbiteCavesSeason;
import model.game.Board;
import model.game.minigame.DeadLineRule;
import model.game.minigame.SaveOurSeedsRule;
import model.game.minigame.SpecialStageRule;
import model.game.plant.Plant;

/**
 * The bits of a stage that live on the lawn rather than in the HUD: the cells Save Our Seeds is
 * guarding, the line Deadline zombies must not cross, and each chapter's own markers.
 *
 * <p>Nothing here decides when an event happens or how long it lasts; the seasons record what they
 * did and this fades it out over the window they say it is worth showing for.
 */
public final class StageRuleRenderer implements WorldRenderer {

  private static final float DEADLINE_WIDTH = 4f;
  private static final float TIDE_LINE_WIDTH = 4f;
  private static final float TORNADO_TRAIL_HEIGHT = 7f;
  private static final float SWIRL_SPEED = 260f;

  private final LawnGeometry geometry;
  private final Color guardFill = new Color(0.35f, 1f, 0.45f, 0.22f);
  private final Color guardEdge = new Color(0.45f, 1f, 0.55f, 0.95f);
  private final Color deadline = new Color(1f, 0.2f, 0.18f, 0.85f);
  private final Color sandSwirl = new Color(0.98f, 0.82f, 0.42f, 1f);
  private final Color sandTrail = new Color(0.92f, 0.72f, 0.30f, 1f);
  private final Color windBand = new Color(0.88f, 0.96f, 1f, 1f);
  private final Color windGust = new Color(1f, 1f, 1f, 1f);
  private final Color tideLine = new Color(0.30f, 0.82f, 1f, 0.95f);
  private final Color coastFill = new Color(1f, 0.90f, 0.58f, 0.20f);
  private final Color coastEdge = new Color(1f, 0.84f, 0.42f, 0.70f);

  private float clock;

  public StageRuleRenderer(LawnGeometry geometry) {
    this.geometry = geometry;
  }

  @Override
  public void render(RenderContext context, GameManager game, float delta) {
    if (game == null || game.getBoard() == null) {
      return;
    }
    clock += delta;
    SpecialStageRule rule = game.getSpecialStageRule();
    if (rule instanceof SaveOurSeedsRule guarded) {
      drawGuardedCells(context, guarded);
    } else if (rule instanceof DeadLineRule line) {
      drawDeadline(context, line, game);
    }
    drawSeasonMarkers(context, game);
  }

  private void drawSeasonMarkers(RenderContext context, GameManager game) {
    if (game.getSeason() instanceof AncientEgyptSeason egypt) {
      drawTornadoes(context, egypt, game.getCurrentTick());
    } else if (game.getSeason() instanceof FrostbiteCavesSeason frost) {
      drawIceWind(context, frost, game);
    } else if (game.getSeason() instanceof BigWaveBeachSeason beach) {
      drawTideMarkers(context, beach, game.getBoard());
    }
  }

  /** A swirl where the zombie was picked up, a trail to where it was dropped, an arrow on the end. */
  private void drawTornadoes(RenderContext context, AncientEgyptSeason egypt, int currentTick) {
    var events = egypt.getRecentTornadoes();
    if (events.isEmpty()) {
      return;
    }
    Gdx.gl.glEnable(GL20.GL_BLEND);
    ShapeRenderer shapes = context.getShapes();

    shapes.begin(ShapeRenderer.ShapeType.Filled);
    for (AncientEgyptSeason.TornadoEvent event : events) {
      float fade = fadeOf(currentTick - event.tick(), AncientEgyptSeason.TORNADO_EVENT_TICKS);
      if (fade <= 0f) {
        continue;
      }
      float y = geometry.rowCentreY(event.row());
      float from = onLawn(event.fromColumn());
      float to = onLawn(event.toColumn());
      shapes.setColor(sandTrail.r, sandTrail.g, sandTrail.b, 0.9f * fade);
      shapes.rect(Math.min(from, to), y - TORNADO_TRAIL_HEIGHT / 2f,
          Math.abs(to - from), TORNADO_TRAIL_HEIGHT);
      // The head points the way the zombie was carried, towards the house.
      float head = geometry.getCellWidth() * 0.22f;
      shapes.setColor(sandSwirl.r, sandSwirl.g, sandSwirl.b, fade);
      shapes.triangle(to - head, y, to, y + head * 0.55f, to, y - head * 0.55f);
    }
    shapes.end();

    shapes.begin(ShapeRenderer.ShapeType.Line);
    for (AncientEgyptSeason.TornadoEvent event : events) {
      float fade = fadeOf(currentTick - event.tick(), AncientEgyptSeason.TORNADO_EVENT_TICKS);
      if (fade <= 0f) {
        continue;
      }
      float y = geometry.rowCentreY(event.row());
      swirl(shapes, onLawn(event.fromColumn()), y, 1f, fade);
      swirl(shapes, onLawn(event.toColumn()), y, 0.6f, fade);
    }
    shapes.end();
  }

  /** A zombie is picked up at the spawn column, off the right edge, so it is pulled onto the lawn. */
  private float onLawn(double column) {
    return geometry.columnCentreX(Math.min(column, geometry.getColumns() - 1.0));
  }

  private void swirl(ShapeRenderer shapes, float x, float y, float scale, float fade) {
    shapes.setColor(sandSwirl.r, sandSwirl.g, sandSwirl.b, fade);
    for (int ring = 1; ring <= 3; ring++) {
      float radius = geometry.getCellHeight() * 0.15f * ring * scale;
      float spin = clock * SWIRL_SPEED * (ring % 2 == 0 ? -1f : 1f) + ring * 40f;
      // Twice, a pixel apart, because ShapeRenderer lines are always one pixel wide.
      shapes.arc(x, y, radius, spin, 250f);
      shapes.arc(x, y, radius + 1.5f, spin, 250f);
    }
  }

  /** A band over the row the wind hit, with a front that sweeps across it once. */
  private void drawIceWind(RenderContext context, FrostbiteCavesSeason frost, GameManager game) {
    int row = frost.getWindRow();
    if (row < 0 || row >= game.getBoard().getRows()) {
      return;
    }
    int age = game.getCurrentTick() - frost.getWindTick();
    float fade = fadeOf(age, FrostbiteCavesSeason.WIND_EVENT_TICKS);
    if (fade <= 0f) {
      return;
    }

    float left = geometry.columnToX(0);
    float width = geometry.getCellWidth() * game.getBoard().getColumns();
    float bottom = geometry.rowToY(row);
    float height = geometry.getCellHeight();
    float progress = Math.min(1f, age / (float) FrostbiteCavesSeason.WIND_EVENT_TICKS);
    float frontX = left + width * (1f - progress);

    Gdx.gl.glEnable(GL20.GL_BLEND);
    ShapeRenderer shapes = context.getShapes();
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    // The caves are already ice-blue, so the gust has to be near-white to read at all.
    shapes.setColor(windBand.r, windBand.g, windBand.b, 0.45f * fade);
    shapes.rect(left, bottom, width, height);
    shapes.setColor(windGust.r, windGust.g, windGust.b, 0.85f * fade);
    shapes.rect(Math.max(left, frontX - geometry.getCellWidth() * 0.55f), bottom,
        Math.min(geometry.getCellWidth() * 0.55f, frontX - left), height);
    float edge = Math.max(2f, height * 0.06f);
    shapes.setColor(windGust.r, windGust.g, windGust.b, 0.95f * fade);
    shapes.rect(left, bottom, width, edge);
    shapes.rect(left, bottom + height - edge, width, edge);
    shapes.end();

    shapes.begin(ShapeRenderer.ShapeType.Line);
    shapes.setColor(windGust.r, windGust.g, windGust.b, fade);
    for (int streak = 0; streak < 5; streak++) {
      float y = bottom + height * (0.22f + 0.14f * streak);
      float tail = geometry.getCellWidth() * (0.8f + 0.6f * ((streak + progress * 3f) % 2f));
      float x = frontX + geometry.getCellWidth() * 0.25f * (streak % 3);
      shapes.line(Math.max(left, x - tail), y, Math.min(left + width, x), y);
    }
    shapes.end();
  }

  /** The line the sea can never pass, and the coast strip between it and low tide. */
  private void drawTideMarkers(RenderContext context, BigWaveBeachSeason beach, Board board) {
    int maxColumn = beach.getMaxWaterColumn(board.getColumns());
    int lowColumn = beach.getLowTideColumn(board.getColumns());
    if (maxColumn >= board.getColumns()) {
      return;
    }
    float bottom = geometry.rowToY(board.getRows() - 1);
    float height = geometry.getCellHeight() * board.getRows();
    float lineX = geometry.columnToX(maxColumn);
    float coastWidth = geometry.getCellWidth() * Math.max(0, lowColumn - maxColumn);

    Gdx.gl.glEnable(GL20.GL_BLEND);
    ShapeRenderer shapes = context.getShapes();
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    if (coastWidth > 0f) {
      shapes.setColor(coastFill);
      shapes.rect(lineX, bottom, coastWidth, height);
    }
    shapes.setColor(tideLine);
    shapes.rect(lineX - TIDE_LINE_WIDTH / 2f, bottom, TIDE_LINE_WIDTH, height);
    shapes.end();

    if (coastWidth <= 0f) {
      return;
    }
    shapes.begin(ShapeRenderer.ShapeType.Line);
    shapes.setColor(coastEdge);
    for (int row = 0; row < board.getRows(); row++) {
      shapes.rect(lineX + 2f, geometry.rowToY(row) + 2f,
          coastWidth - 4f, geometry.getCellHeight() - 4f);
    }
    shapes.end();
  }

  /** 1 right after the event, 0 once the season stops reporting it. */
  private static float fadeOf(int age, int window) {
    if (age < 0 || age > window) {
      return 0f;
    }
    return 1f - (age / (float) window);
  }

  // Pulsing, because "keep these alive" has to read differently from ordinary decoration.
  private void drawGuardedCells(RenderContext context, SaveOurSeedsRule rule) {
    if (rule.getProtectedPlants().isEmpty()) {
      return;
    }
    float pulse = 0.7f + 0.3f * (float) Math.sin(clock * 3f);
    ShapeRenderer shapes = context.getShapes();
    Gdx.gl.glEnable(GL20.GL_BLEND);

    shapes.begin(ShapeRenderer.ShapeType.Filled);
    shapes.setColor(guardFill.r, guardFill.g, guardFill.b, guardFill.a * pulse);
    for (Plant plant : rule.getProtectedPlants()) {
      if (!plant.isDead()) {
        cell(shapes, plant);
      }
    }
    shapes.end();

    shapes.begin(ShapeRenderer.ShapeType.Line);
    shapes.setColor(guardEdge.r, guardEdge.g, guardEdge.b, guardEdge.a * pulse);
    for (Plant plant : rule.getProtectedPlants()) {
      if (!plant.isDead()) {
        cell(shapes, plant);
      }
    }
    shapes.end();
  }

  private void cell(ShapeRenderer shapes, Plant plant) {
    shapes.rect(geometry.columnToX(plant.getCol()) + 1f, geometry.rowToY(plant.getRow()) + 1f,
        geometry.getCellWidth() - 2f, geometry.getCellHeight() - 2f);
  }

  /** The rule loses the moment a zombie's column reaches this one, so the line sits on its edge. */
  private void drawDeadline(RenderContext context, DeadLineRule rule, GameManager game) {
    float x = geometry.columnToX(rule.getDeadlineColumn());
    float bottom = geometry.rowToY(game.getBoard().getRows() - 1);
    float height = geometry.getCellHeight() * game.getBoard().getRows();

    ShapeRenderer shapes = context.getShapes();
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    shapes.setColor(deadline);
    shapes.rect(x - DEADLINE_WIDTH / 2f, bottom, DEADLINE_WIDTH, height);
    shapes.end();
  }

  @Override
  public void dispose() {
  }
}
