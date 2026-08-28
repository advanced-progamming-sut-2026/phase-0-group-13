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


/**
 * One place to get the Scene2D Skin, so the choice only gets made once.
 *
 * <p>Open item 3 in docs/phase2/asset-organization.md was the reason this class exists: the skin
 * exists twice, once in {@code resources/skin/} and once inside the pvz-skin jar, and two files at
 * the classpath path {@code skin/pvz2_skin.json} is a silent bug that depends on jar order. The
 * repo's own copy wins, because it is the newer of the two — the published jar is missing the
 * CheckBox and SelectBox styles. The jar is not a dependency, so nothing else claims that path.
 * build.gradle copies {@code resources/skin/} there at build time rather than us keeping a third
 * copy under {@code assets/}.
 *
 * <p>A skin uploads a texture atlas, so it needs the GL context. Don't call get() from a static
 * field or a constructor that runs before the app starts.
 */
public final class UiSkinProvider implements Disposable {

  /** Where processResources puts resources/skin. */
  private static final String SKIN_PATH = "skin/pvz2_skin.json";

  /** Style names, so a typo shows up here and not as a GdxRuntimeException in a screen. */
  public static final String LABEL_BIG = "big";

  public static final String LABEL_MEDIUM = "medium";
  /** Outlined variants, for type that sits on artwork rather than on a panel. */
  public static final String LABEL_BIG_OUTLINE = "big_outline";
  public static final String LABEL_MEDIUM_OUTLINE = "medium_outline";
  public static final String BUTTON_GREEN = "green";
  public static final String BUTTON_BROWN = "brown";
  public static final String BUTTON_PURPLE = "purple";
  /** The flat inner surface: a well inside a frame, or a plain backing on its own. */
  public static final String PANEL_BACKGROUND = "image_ui_dialog_asset_inner_bkgd_10";
  /** The raised dialog frame that goes around a panel. */
  public static final String PANEL_FRAME = "image_ui_dialog_asset_dialogborder_10";
  /** A 1x1 white pixel, for scrims, dividers and row striping. Tint it at the call site. */
  public static final String WHITE_PIXEL = "white_pixel";
  public static final String DIALOG_BORDER = "image_ui_dialog_asset_dialogborder_10";
  public static final String MODAL_DIM = "modal_background";
  public static final String COIN_ICON = "image_ui_generic_coin_icon_small";
  public static final String GEM_ICON = "image_ui_generic_gem_icon_small";
  /** Dark pill behind a currency readout, so a number reads on any backdrop. */
  public static final String COUNTER_PLATE = "image_ui_generic_counter_bg";
  /** The store's own "limited offer" banner. */
  public static final String PROMO_RIBBON = "image_ui_cards_store_promo_ribbon";
  /** The almanac's own pages: green for the plant side, purple for the zombie side. */
  public static final String ALMANAC_PLANT_CARD = "image_ui_cards_almanac_plant_card";
  public static final String ALMANAC_ZOMBIE_CARD = "image_ui_cards_almanac_zombie_card";

  /**
   * The Travel Log's own quest panels.
   *
   * <p>A pair of surfaces -- plain cream, and cream under a blue header for an epic challenge --
   * plus a green outline frame that goes over either one once the quest is done. The frames have
   * hollow centres, so they layer rather than replace.
   */
  public static final String QUEST_PANEL = "image_ui_quests_travel_log_panel_default";
  public static final String QUEST_PANEL_EPIC = "image_ui_quests_travel_log_panel_epic";
  public static final String QUEST_PANEL_DONE = "image_ui_quests_travel_log_panel_complete";
  public static final String QUEST_PANEL_EPIC_DONE =
      "image_ui_quests_travel_log_panel_epic_complete";
  /** The Travel Log's own currency icons; chunkier than the tiny generic pair. */
  public static final String QUEST_COIN_ICON = "image_ui_quests_coin_icon";
  public static final String QUEST_GEM_ICON = "image_ui_quests_gem_icon";
  /** Padlocks, for anything the player has not earned yet. */
  public static final String LOCK_ICON = "image_ui_lock_small";
  public static final String LOCK_ICON_GOLD = "image_ui_lock_small_gold";

  private Skin skin;
  private boolean loadFailed;

  /**
   * The shared skin, or null if it could not be loaded. Check isAvailable() first: a screen that
   * can't get a skin should draw nothing rather than take the whole app down.
   */
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

  /** Whether get() will give back something usable. */
  public boolean isAvailable() {
    return get() != null;
  }

  @Override
  public void dispose() {
    if (skin != null) {
      skin.dispose();
      skin = null;
    }
  }

  /**
   * A Skin that can load the FreeType fonts a Skin Composer export embeds in the skin json.
   *
   * <p>The plain loader can't instantiate a FreeTypeFontGenerator, it has no no-arg constructor,
   * so it dies on the generator block at the top of the file. This is the standard Skin Composer
   * recipe for that: read the block, generate the BitmapFont from the sibling ttf, register it
   * under the block's name.
   */
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
              // The fields a FreeTypeFontParameter can't deserialise on its own come out first.
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

              FreeTypeFontGenerator generator =
                  new FreeTypeFontGenerator(skinFile.parent().child(path));
              BitmapFont font = generator.generateFont(parameter);
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
  }
}
