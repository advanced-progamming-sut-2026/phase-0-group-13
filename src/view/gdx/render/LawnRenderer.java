package view.gdx.render;

import model.core.GameManager;


/**
 * Draws the static part of a match: background, lane grid, water and tile effects.
 *
 * <p>The graphical version of the grid half of view.BoardRenderer. That class is left alone and
 * still does the terminal version.
 *
 * <p>Empty for now since we have no textures yet. See open item 1 in
 * docs/phase2/asset-organization.md.
 */
public final class LawnRenderer implements WorldRenderer {

  private final LawnGeometry geometry;

  public LawnRenderer(LawnGeometry geometry) {
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
    // TODO background for game.getSeason()
    // TODO lane grid, using geometry
    // TODO water tiles from board.isWaterAt(row, col)
    // TODO tile effects from board.getTile(row, col).getEffect()
  }

  @Override
  public void dispose() {
    // TODO free the background and tile textures once the loader owns them
  }
}
