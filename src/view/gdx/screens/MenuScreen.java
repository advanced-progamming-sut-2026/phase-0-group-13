package view.gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import view.gdx.audio.GameAudio;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.CurrencyHud;
import view.gdx.ui.DebugPanel;
import view.gdx.ui.Toast;
import view.gdx.ui.UiSkinProvider;


/**
 * Base for every Scene2D menu: the header, the currency readout, the toast and the debug panel are
 * built once here so no screen carries its own copy.
 *
 * <p>A subclass only says what it is called and what goes in the middle. Everything a menu shares
 * with the other menus — including Escape going back — is handled here.
 *
 * <p>Own ScreenViewport rather than the shared world one, same reason HudStage has one: the world
 * is letterboxed so the lawn keeps its shape, but a menu wants one unit per real pixel so the text
 * stays sharp.
 */
public abstract class MenuScreen extends BaseScreen {

  protected Stage stage;
  protected Skin skin;

  private String notice;
  private TextureAtlas backgroundAtlas;

  /** Backdrop atlas under assets/, or null for none. Needs a region called "texture". */
  protected String backgroundAtlasPath() {
    return null;
  }

  protected MenuScreen(PvzGdxGame game) {
    super(game);
  }

  /**
   * Queues a message to toast as soon as this screen is shown.
   *
   * <p>For the cases where the thing worth reporting happens on the screen you are leaving: the
   * old stage is disposed on the way out, so its toast would never be seen.
   */
  public MenuScreen withNotice(String message) {
    this.notice = message;
    return this;
  }

  /**
   * The background track this screen plays. Menus share one, so only a screen that wants
   * something else overrides it; asking for the track already playing leaves it running.
   */
  protected GameAudio.Track musicTrack() {
    return GameAudio.Track.MENU;
  }

  /** Shown top left. */
  protected abstract String title();

  /** Fills the middle of the screen. Only called when there is a skin. */
  protected abstract void buildContent(Table content);

  /**
   * Where Escape and the Back button go, or null if this screen is the root.
   *
   * <p>Every menu overrides this, which is what stops a screen from becoming a dead end.
   */
  protected Screen backTarget() {
    return null;
  }

  @Override
  public void show() {
    GameAudio.getInstance().playMusic(musicTrack());
    stage = new Stage(new ScreenViewport());
    skin = game.getUiSkin().get();
    Gdx.input.setInputProcessor(stage);
    if (skin == null) {
      // No skin means nothing to draw, but the window should stay up rather than take the app
      // down. UiSkinProvider has already logged why.
      return;
    }

    addBackground();

    Table root = new Table();
    root.setFillParent(true);
    root.pad(24f);
    stage.addActor(root);

    Table header = new Table();
    header.add(new Label(title(), skin, UiSkinProvider.LABEL_BIG)).left().expandX();
    header.add(new CurrencyHud(skin)).right();
    root.add(header).growX().padBottom(20f).row();

    Table content = new Table();
    content.defaults().pad(6f);
    root.add(content).grow().row();
    buildContent(content);

    if (DebugPanel.isEnabled()) {
      // Floats over the corner instead of taking a row - it can be toggled on any screen now,
      // and the taller ones have no spare height to give it.
      Table corner = new Table();
      corner.setFillParent(true);
      corner.bottom().right().pad(12f);
      corner.add(new DebugPanel(skin, this::toast));
      stage.addActor(corner);
    }

    stage.addListener(
        new InputListener() {
          @Override
          public boolean keyDown(InputEvent event, int keycode) {
            if (keycode != Input.Keys.ESCAPE) {
              return false;
            }
            Screen back = backTarget();
            if (back != null) {
              go(back);
            }
            return true;
          }
        });

    if (notice != null) {
      toast(notice);
      notice = null;
    }
  }

  /** Added before the root table so everything else draws on top. */
  private void addBackground() {
    String path = backgroundAtlasPath();
    if (path == null || !Gdx.files.internal(path).exists()) {
      return;
    }
    backgroundAtlas = new TextureAtlas(Gdx.files.internal(path));
    TextureRegion region = backgroundAtlas.findRegion("texture");
    if (region == null) {
      return;
    }
    Image background = new Image(region);
    background.setFillParent(true);
    background.setScaling(Scaling.fill);
    stage.addActor(background);
  }

  /** A panel-backed table, for the screens that hold a form. */
  protected Table panel() {
    Table panel = new Table();
    panel.setBackground(skin.getDrawable(UiSkinProvider.PANEL_BACKGROUND));
    panel.pad(28f);
    panel.defaults().pad(6f);
    return panel;
  }

  /** Adds a "label: [input]" row and gives back the input. */
  protected TextField field(Table form, String label, boolean password) {
    TextField input = new TextField("", skin);
    if (password) {
      input.setPasswordMode(true);
      input.setPasswordCharacter('*');
    }
    form.add(new Label(label, skin, UiSkinProvider.LABEL_MEDIUM)).right().padRight(14f);
    form.add(input).width(340f).height(52f).row();
    return input;
  }

  protected TextButton button(String text, String style, Runnable action) {
    TextButton button = new TextButton(text, skin, style);
    button.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            GameAudio.getInstance().play(GameAudio.Sfx.CLICK);
            action.run();
          }
        });
    return button;
  }

  /** Swaps to another screen. Goes through the game so the old one gets disposed. */
  protected void go(Screen next) {
    game.switchScreen(next);
  }

  /** Temporary message, see Toast. */
  protected void toast(String message) {
    Toast.show(stage, skin, message);
  }

  @Override
  public void render(float delta) {
    stage.act(delta);
    stage.getViewport().apply();
    stage.draw();
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);
    stage.getViewport().update(width, height, true);
  }

  @Override
  public void hide() {
    Gdx.input.setInputProcessor(null);
  }

  @Override
  public void dispose() {
    if (stage != null) {
      stage.dispose();
      stage = null;
    }
    if (backgroundAtlas != null) {
      backgroundAtlas.dispose();
      backgroundAtlas = null;
    }
  }
}
