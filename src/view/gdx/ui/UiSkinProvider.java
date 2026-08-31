package view.gdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.Hinting;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import view.gdx.core.GdxConfig;


public final class UiSkinProvider implements Disposable {

  private static final String SKIN_PATH = "skin/pvz2_skin.json";

  public static final String LABEL_BIG = "big";

  public static final String LABEL_MEDIUM = "medium";
  public static final String LABEL_BIG_OUTLINE = "big_outline";
  public static final String LABEL_MEDIUM_OUTLINE = "medium_outline";
  public static final String BUTTON_GREEN = "green";
  public static final String BUTTON_BROWN = "brown";
  public static final String BUTTON_PURPLE = "purple";
  public static final String PANEL_BACKGROUND = "image_ui_dialog_asset_inner_bkgd_10";
  public static final String PANEL_FRAME = "image_ui_dialog_asset_dialogborder_10";
  public static final String WHITE_PIXEL = "white_pixel";
  public static final String DIALOG_BORDER = "image_ui_dialog_asset_dialogborder_10";
  public static final String MODAL_DIM = "modal_background";
  public static final String COIN_ICON = "image_ui_generic_coin_icon_small";
  public static final String GEM_ICON = "image_ui_generic_gem_icon_small";
  public static final String COUNTER_PLATE = "image_ui_generic_counter_bg";
  public static final String PROMO_RIBBON = "image_ui_cards_store_promo_ribbon";
  public static final String ALMANAC_PLANT_CARD = "image_ui_cards_almanac_plant_card";
  public static final String ALMANAC_ZOMBIE_CARD = "image_ui_cards_almanac_zombie_card";

  public static final String QUEST_PANEL = "image_ui_quests_travel_log_panel_default";
  public static final String QUEST_PANEL_EPIC = "image_ui_quests_travel_log_panel_epic";
  public static final String QUEST_PANEL_DONE = "image_ui_quests_travel_log_panel_complete";
  public static final String QUEST_PANEL_EPIC_DONE =
      "image_ui_quests_travel_log_panel_epic_complete";
  public static final String QUEST_COIN_ICON = "image_ui_quests_coin_icon";
  public static final String QUEST_GEM_ICON = "image_ui_quests_gem_icon";
  public static final String LOCK_ICON = "image_ui_lock_small";
  public static final String LOCK_ICON_GOLD = "image_ui_lock_small_gold";

  private Skin skin;
  private boolean loadFailed;

  public Skin get() {
    if (skin == null && !loadFailed) {
      try {
        skin = new FreeTypeSkin(Gdx.files.classpath(SKIN_PATH));
      } catch (RuntimeException e) {
        loadFailed = true;
        Gdx.app.error("UiSkinProvider", "could not load the pvz2 skin from " + SKIN_PATH, e);
      }
    }
    return skin;
  }

  public boolean isAvailable() {
    return get() != null;
  }

  public static float fontScale(float factor) {
    return factor / GdxConfig.textSupersample();
  }

  @Override
  public void dispose() {
    if (skin != null) {
      skin.dispose();
      skin = null;
    }
  }

  private static final class FreeTypeSkin extends Skin {

    private FreeTypeSkin(FileHandle skinFile) {
      super(skinFile);
    }

    @Override
    protected Json getJsonLoader(final FileHandle skinFile) {
      Json json = super.getJsonLoader(skinFile);
      final Skin skin = this;
      json.setSerializer(
          FreeTypeFontGenerator.class,
          new Json.ReadOnlySerializer<FreeTypeFontGenerator>() {
            @Override
            @SuppressWarnings("rawtypes")
            public FreeTypeFontGenerator read(Json json, JsonValue data, Class type) {
              String path = json.readValue("font", String.class, data);
              data.remove("font");
              Hinting hinting =
                  Hinting.valueOf(json.readValue("hinting", String.class, "AutoMedium", data));
              data.remove("hinting");
              TextureFilter minFilter =
                  TextureFilter.valueOf(json.readValue("minFilter", String.class, "Nearest", data));
              data.remove("minFilter");
              TextureFilter magFilter =
                  TextureFilter.valueOf(json.readValue("magFilter", String.class, "Nearest", data));
              data.remove("magFilter");

              FreeTypeFontParameter parameter = json.readValue(FreeTypeFontParameter.class, data);
              parameter.hinting = hinting;
              parameter.minFilter = minFilter;
              parameter.magFilter = magFilter;

              float supersample = GdxConfig.textSupersample();
              if (supersample > 1.01f) {
                bakeLarger(parameter, supersample);
              }

              FreeTypeFontGenerator generator =
                  new FreeTypeFontGenerator(skinFile.parent().child(path));
              BitmapFont font = generator.generateFont(parameter);
              if (supersample > 1.01f) {
                font.getData().setScale(1f / supersample);
              }
              skin.add(data.name, font);
              if (parameter.incremental) {
                generator.dispose();
                return null;
              }
              return generator;
            }
          });
      return json;
    }

    private static void bakeLarger(FreeTypeFontParameter parameter, float scale) {
      parameter.size = Math.round(parameter.size * scale);
      parameter.borderWidth *= scale;
      parameter.shadowOffsetX = Math.round(parameter.shadowOffsetX * scale);
      parameter.shadowOffsetY = Math.round(parameter.shadowOffsetY * scale);
      parameter.spaceX = Math.round(parameter.spaceX * scale);
      parameter.spaceY = Math.round(parameter.spaceY * scale);
      parameter.genMipMaps = true;
      parameter.minFilter = TextureFilter.MipMapLinearLinear;
      parameter.magFilter = TextureFilter.Linear;
    }
  }
}
