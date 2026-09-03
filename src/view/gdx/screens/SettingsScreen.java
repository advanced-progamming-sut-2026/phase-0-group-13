package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import data.persistence.UserManager;
import model.account.User;
import view.gdx.audio.GameAudio;
import view.gdx.core.GameSettings;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.UiSkinProvider;


public final class SettingsScreen extends MenuScreen {

  private static final String[] LEVELS = {"1", "2", "3", "4", "5"};
  private static final String[] SPEEDS = {"1", "2", "3"};

  public SettingsScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  protected String title() {
    return "Settings";
  }

  @Override
  protected Screen backTarget() {
    return new MainMenuScreen(game);
  }

  /** The rows that take effect the moment they are touched, rather than waiting for Apply. */
  private void addAudioAndToggleRows(Table panel) {
    panel.add(new Label("Music", skin, UiSkinProvider.LABEL_MEDIUM)).right().padRight(14f);
    panel.add(volumeSlider(GameSettings.getMusicVolume(), GameSettings::setMusicVolume))
        .width(200f).height(46f).row();

    panel.add(new Label("Sound effects", skin, UiSkinProvider.LABEL_MEDIUM))
        .right().padRight(14f);
    panel.add(volumeSlider(GameSettings.getSfxVolume(), volume -> {
      GameSettings.setSfxVolume(volume);
      GameAudio.getInstance().play(GameAudio.Sfx.CLICK);
    })).width(200f).height(46f).row();

    panel.add(new Label("Mute", skin, UiSkinProvider.LABEL_MEDIUM)).right().padRight(14f);
    panel.add(toggle(GameSettings.isMuted(), () -> {
      GameSettings.setMuted(!GameSettings.isMuted());
      GameAudio.getInstance().refreshVolumes();
    })).width(200f).height(46f).row();

    panel.add(new Label("Grid overlay", skin, UiSkinProvider.LABEL_MEDIUM)).right().padRight(14f);
    panel.add(toggle(GameSettings.isGridVisible(),
        () -> GameSettings.setGridVisible(!GameSettings.isGridVisible())))
        .width(200f).height(46f).row();

    panel.add(new Label("Debug mode", skin, UiSkinProvider.LABEL_MEDIUM)).right().padRight(14f);
    panel.add(toggle(GameSettings.isDebugMode(),
        () -> GameSettings.setDebugMode(!GameSettings.isDebugMode())))
        .width(200f).height(46f).row();
  }

  @Override
  protected void buildContent(Table content) {
    User user = UserManager.getInstance().getCurrentUser();
    Table panel = panel();

    if (user == null) {
      panel.add(new Label("error: no user logged in", skin, UiSkinProvider.LABEL_MEDIUM)).row();
      panel.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget()))).width(220f);
      content.add(panel);
      return;
    }

    SelectBox<String> difficulty = new SelectBox<>(skin);
    difficulty.setItems(LEVELS);
    difficulty.setSelected(String.valueOf(user.getDifficultyLevel()));
    panel.add(new Label("Difficulty", skin, UiSkinProvider.LABEL_MEDIUM)).right().padRight(14f);
    panel.add(difficulty).width(200f).height(46f).row();

    SelectBox<String> speed = new SelectBox<>(skin);
    speed.setItems(SPEEDS);
    speed.setSelected(String.valueOf(GameSettings.getGameSpeed()));
    panel.add(new Label("Game speed", skin, UiSkinProvider.LABEL_MEDIUM)).right().padRight(14f);
    panel.add(speed).width(200f).height(46f).row();

    addAudioAndToggleRows(panel);

    Table actions = new Table();
    actions.defaults().pad(6f).width(200f).height(56f);
    actions.add(button("Apply", UiSkinProvider.BUTTON_GREEN, () -> apply(user, difficulty, speed)));
    actions.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())));
    panel.add(actions).colspan(2).padTop(10f).row();
    content.add(panel);
  }

  private Slider volumeSlider(float initial, FloatSetter apply) {
    Slider slider = new Slider(GameSettings.MIN_VOLUME, GameSettings.MAX_VOLUME, 0.05f, false,
        skin);
    slider.setValue(initial);
    slider.addListener(new ChangeListener() {
      @Override
      public void changed(ChangeEvent event, Actor actor) {
        apply.set(slider.getValue());
        GameAudio.getInstance().refreshVolumes();
      }
    });
    return slider;
  }

  @FunctionalInterface
  private interface FloatSetter {
    void set(float value);
  }

  private com.badlogic.gdx.scenes.scene2d.ui.TextButton toggle(boolean on, Runnable flip) {
    return button(on ? "On" : "Off",
        on ? UiSkinProvider.BUTTON_GREEN : UiSkinProvider.BUTTON_BROWN,
        () -> {
          flip.run();
          go(new SettingsScreen(game));
        });
  }

  private void apply(User user, SelectBox<String> difficulty, SelectBox<String> speed) {
    GameSettings.setGameSpeed(Integer.parseInt(speed.getSelected()));
    user.setDifficultyLevel(Integer.parseInt(difficulty.getSelected()));
    String difficultyChoice = difficulty.getSelected();
    String speedChoice = speed.getSelected();
    runAsync(
        () -> {
          UserManager.getInstance().updateCurrentUserGameState();
          return null;
        },
        ignored -> toast("Difficulty " + difficultyChoice + ", speed x" + speedChoice + "."),
        e -> toast(e.getMessage()));
  }
}
