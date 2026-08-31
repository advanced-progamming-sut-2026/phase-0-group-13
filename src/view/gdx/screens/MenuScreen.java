package view.gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import view.gdx.audio.GameAudio;
import view.gdx.core.GdxConfig;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.ButtonFeel;
import view.gdx.ui.CurrencyHud;
import view.gdx.ui.DebugPanel;
import view.gdx.ui.LayeredDrawable;
import view.gdx.ui.Toast;
import view.gdx.ui.UiSkinProvider;


public abstract class MenuScreen extends BaseScreen {

  protected Stage stage;
  protected Skin skin;

  @Override
  public Stage uiStage() {
    return stage;
  }

  private String notice;
  private TextureAtlas backgroundAtlas;
  private Texture backgroundTexture;
  private SpriteBatch backdropBatch;

  private static final float SCRIM_ALPHA = 0.42f;

  private static final float ENTRANCE_RISE = 18f;

  private static final float ENTRANCE_SECONDS = 0.18f;

  private static final String DEFAULT_BACKGROUND = "textures/environment/darkagesseason.atlas";

  protected String backgroundAtlasPath() {
    return DEFAULT_BACKGROUND;
  }

  protected String backgroundImagePath() {
    return null;
  }

  protected boolean scrimBackground() {
    return true;
  }

  protected MenuScreen(PvzGdxGame game) {
    super(game);
  }

  public MenuScreen withNotice(String message) {
    this.notice = message;
    return this;
  }

  protected GameAudio.Track musicTrack() {
    return GameAudio.Track.MENU;
  }

  protected abstract String title();

  protected abstract void buildContent(Table content);

  protected Screen backTarget() {
    return null;
  }

  protected void onEscape() {
    Screen back = backTarget();
    if (back != null) {
      go(back);
    }
  }

  @Override
  public void show() {
    GameAudio.getInstance().playMusic(musicTrack());
    stage = new Stage(new ExtendViewport(GdxConfig.WORLD_WIDTH, GdxConfig.WORLD_HEIGHT));
    skin = game.getUiSkin().get();
    Gdx.input.setInputProcessor(stage);
    if (skin == null) {
      return;
    }

    addBackground();

    Table root = new Table();
    root.setFillParent(true);
    root.pad(24f);
    stage.addActor(root);

    Table header = new Table();
    header.add(new Label(title(), skin, UiSkinProvider.LABEL_BIG_OUTLINE)).left().expandX();
    header.add(new CurrencyHud(skin)).right();
    root.add(header).growX().padBottom(20f).row();

    Table content = new Table();
    content.defaults().pad(6f);
    root.add(content).grow().row();
    buildContent(content);

    playEntrance(root);

    if (DebugPanel.isEnabled()) {
      Table corner = new Table();
      corner.setFillParent(true);
      corner.bottom().left().pad(12f);
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
            onEscape();
            return true;
          }
        });

    if (notice != null) {
      toast(notice);
      notice = null;
    }
  }

  private void addBackground() {
    String image = backgroundImagePath();
    if (image != null && Gdx.files.internal(image).exists()) {
      backgroundTexture = new Texture(Gdx.files.internal(image));
      backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
      // Drawn across the whole window in render() rather than as an actor, so it covers the
      // letterbox as well and never leaves bars around itself.
      if (scrimBackground()) {
        addScrim();
      }
      return;
    }
    String path = backgroundAtlasPath();
    if (path == null || !Gdx.files.internal(path).exists()) {
      return;
    }
    backgroundAtlas = new TextureAtlas(Gdx.files.internal(path));
    TextureRegion region = backgroundAtlas.findRegion("texture");
    if (region == null) {
      return;
    }
    addBackdrop(region);
  }

  private void addBackdrop(TextureRegion region) {
    Image background = new Image(region);
    background.setFillParent(true);
    background.setScaling(Scaling.fill);
    stage.addActor(background);
    if (scrimBackground()) {
      addScrim();
    }
  }

  private void addScrim() {
    Image scrim = new Image(skin.newDrawable(UiSkinProvider.WHITE_PIXEL,
        new Color(0f, 0f, 0f, SCRIM_ALPHA)));
    scrim.setFillParent(true);
    stage.addActor(scrim);
  }

  private static void playEntrance(Table root) {
    root.getColor().a = 0f;
    root.setTransform(false);
    root.addAction(Actions.parallel(
        Actions.fadeIn(ENTRANCE_SECONDS, Interpolation.fade),
        Actions.sequence(
            Actions.moveBy(0f, -ENTRANCE_RISE),
            Actions.moveBy(0f, ENTRANCE_RISE, ENTRANCE_SECONDS, Interpolation.pow2Out))));
  }

  protected Table panel() {
    Table panel = new Table();
    panel.setBackground(new LayeredDrawable(
        skin.getDrawable(UiSkinProvider.PANEL_BACKGROUND),
        skin.getDrawable(UiSkinProvider.PANEL_FRAME)));
    panel.pad(30f, 34f, 34f, 34f);
    panel.defaults().pad(6f);
    return panel;
  }

  protected Table well() {
    Table well = new Table();
    well.setBackground(skin.getDrawable(UiSkinProvider.PANEL_BACKGROUND));
    well.pad(14f);
    well.defaults().pad(4f);
    return well;
  }

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
    ButtonFeel.apply(button);
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

  protected void go(Screen next) {
    game.switchScreen(next);
  }

  protected void toast(String message) {
    Toast.show(stage, skin, message);
  }

  @Override
  public void render(float delta) {
    drawFullWindowBackdrop();
    stage.act(delta);
    stage.getViewport().apply();
    stage.draw();
  }

  private void drawFullWindowBackdrop() {
    if (backgroundTexture == null) {
      return;
    }
    if (backdropBatch == null) {
      backdropBatch = new SpriteBatch();
    }
    float width = Gdx.graphics.getWidth();
    float height = Gdx.graphics.getHeight();
    float scale = Math.max(width / backgroundTexture.getWidth(),
        height / backgroundTexture.getHeight());
    float drawWidth = backgroundTexture.getWidth() * scale;
    float drawHeight = backgroundTexture.getHeight() * scale;
    HdpiUtils.glViewport(0, 0, (int) width, (int) height);
    backdropBatch.getProjectionMatrix().setToOrtho2D(0f, 0f, width, height);
    backdropBatch.begin();
    backdropBatch.draw(backgroundTexture, (width - drawWidth) / 2f, (height - drawHeight) / 2f,
        drawWidth, drawHeight);
    backdropBatch.end();
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
    if (backgroundTexture != null) {
      backgroundTexture.dispose();
      backgroundTexture = null;
    }
    if (backdropBatch != null) {
      backdropBatch.dispose();
      backdropBatch = null;
    }
  }
}
