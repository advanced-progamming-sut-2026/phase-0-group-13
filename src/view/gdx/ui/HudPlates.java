package view.gdx.ui;

import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * The in-match HUD pieces the skin already carries but nothing was asking for.
 *
 * <p>{@code resources/skin/pvz2_skin.atlas} ships the original game's own HUD set -- the dark
 * rounded plate the counters sit on, the shovel, plant-food and pause buttons, and the wave meter
 * -- and the match HUD was drawing none of it: brown text buttons reading "Shovel" / "Food 0" /
 * "Pause", and counters with nothing behind them, floating on whatever the lawn happened to be
 * painted. Type on artwork is the readability problem the plate exists to solve, so this is where
 * the names live, in one place, resolved leniently: a missing region gives back null and the
 * caller keeps whatever it had.
 */
public final class HudPlates {

  /** The dark rounded plate. 84x42 with a flat middle, so it stretches as a nine-patch. */
  public static final String PLATE = "image_ui_hud_ingame_background_3slice";
  private static final int PLATE_INSET_X = 20;
  private static final int PLATE_INSET_Y = 14;

  public static final String SHOVEL = "image_ui_hud_ingame_shovel_button";
  public static final String SHOVEL_ARMED = "image_ui_hud_ingame_shovel_button_down";
  public static final String PLANT_FOOD = "image_ui_hud_ingame_plantfood_button";
  public static final String PLANT_FOOD_ARMED = "image_ui_hud_ingame_plantfood_button_down";
  public static final String PAUSE = "image_ui_hud_ingame_pause_button";
  public static final String PAUSE_DOWN = "image_ui_hud_ingame_pause_button_down";
  public static final String SUN_ICON = "image_ui_hud_ingame_sun";

  /** The wave meter: a dark trough and the green pill that fills it. */
  public static final String METER = "image_ui_hud_ingame_progress_meter";
  public static final String METER_FILL = "image_ui_hud_ingame_progress_meter_fill";

  private HudPlates() {
  }

  /** Whether the skin has this region at all. */
  public static boolean has(Skin skin, String region) {
    return skin != null && skin.has(region, TextureRegion.class);
  }

  /** A plain stretched drawable for a region, or null when the skin has no such region. */
  public static Drawable drawable(Skin skin, String region) {
    return has(skin, region) ? new TextureRegionDrawable(skin.getRegion(region)) : null;
  }

  /**
   * The counter plate, as a nine-patch so it can be any width without the rounded ends smearing.
   *
   * <p>The splits are not in the atlas -- the art is a plain region, not a {@code .9} -- so they
   * are named here against the piece's own 84x42 corners.
   */
  public static Drawable plate(Skin skin) {
    if (!has(skin, PLATE)) {
      return null;
    }
    return new NinePatchDrawable(new NinePatch(skin.getRegion(PLATE),
        PLATE_INSET_X, PLATE_INSET_X, PLATE_INSET_Y, PLATE_INSET_Y));
  }
}
