package view.gdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import model.core.GameManager;
import model.game.minigame.DeadLineRule;
import model.game.minigame.SaveOurSeedsRule;
import model.game.minigame.SpecialStageRule;
import model.game.plant.Plant;

/**
 * The bits of a special stage that live on the lawn rather than in the HUD: the cells Save Our
 * Seeds is guarding and the line Deadline zombies must not cross.
 *
 * <p>Reads the rule the match is already running, so what is drawn is what will be judged.
 */
public final class StageRuleRenderer implements WorldRenderer {

  private static final float DEADLINE_WIDTH = 4f;

  private final LawnGeometry geometry;
  private final Color guardFill = new Color(0.35f, 1f, 0.45f, 0.22f);
  private final Color guardEdge = new Color(0.45f, 1f, 0.55f, 0.95f);
  private final Color deadline = new Color(1f, 0.2f, 0.18f, 0.85f);

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
