package view.gdx.ui;

import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public final class HudPlates {

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

  public static final String METER = "image_ui_hud_ingame_progress_meter";
  public static final String METER_FILL = "image_ui_hud_ingame_progress_meter_fill";

  private HudPlates() {
  }

  public static boolean has(Skin skin, String region) {
    return skin != null && skin.has(region, TextureRegion.class);
  }

  public static Drawable drawable(Skin skin, String region) {
    return has(skin, region) ? new TextureRegionDrawable(skin.getRegion(region)) : null;
  }

  public static Drawable plate(Skin skin) {
    if (!has(skin, PLATE)) {
      return null;
    }
    return new NinePatchDrawable(new NinePatch(skin.getRegion(PLATE),
        PLATE_INSET_X, PLATE_INSET_X, PLATE_INSET_Y, PLATE_INSET_Y));
  }
}
