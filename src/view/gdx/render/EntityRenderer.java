package view.gdx.render;

import model.core.GameManager;
import model.game.Board;


/**
 * Draws the moving part of a match: plants, zombies, projectiles, suns and lawnmowers.
 *
 * <p>Everything already has a position we can use (Zombie.getX()/getY()/getRow(), Sun.getX(),
 * Projectile) and Board hands us the lists, so nothing in the model has to change to draw them.
 *
 * <p>Empty for now. Static sprites need open item 1, animated ones need open item 2 as well, both
 * in docs/phase2/asset-organization.md.
 */
public final class EntityRenderer implements WorldRenderer {

  private final LawnGeometry geometry;

  public EntityRenderer(LawnGeometry geometry) {
    this.geometry = geometry;
  }

  public LawnGeometry getGeometry() {
    return geometry;
  }

  @Override
  public void render(RenderContext context, GameManager game, float delta) {
    if (game == null || game.getBoard() == null) {
      return;
    }
    Board board = game.getBoard();
    drawPlants(context, board, delta);
    drawZombies(context, board, delta);
    drawProjectiles(context, board);
    drawSuns(context, board);
    drawLawnmowers(context, board);
  }

  private void drawPlants(RenderContext context, Board board, float delta) {
    // TODO for each board.getPlants(), find its tile with geometry and draw the current frame
    // TODO show health, and status effects like frozen, fortified, growth stage
  }

  private void drawZombies(RenderContext context, Board board, float delta) {
    // TODO draw board.getZombies() at columnToX(z.getX()) and rowToY(z.getRow())
    // TODO armour layers on top, plus the walking/eating animation
  }

  private void drawProjectiles(RenderContext context, Board board) {
    // TODO draw board.getProjectiles() at their x/y, lobbed ones need an arc
  }

  private void drawSuns(RenderContext context, Board board) {
    // TODO go through the tiles for suns, falling ones animate and grounded ones pulse
  }

  private void drawLawnmowers(RenderContext context, Board board) {
    // TODO draw board.getLawnmowers() at the left of their lane
  }

  @Override
  public void dispose() {
    // TODO free the entity atlases once the loader owns them
  }
}
