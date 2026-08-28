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


/**
 * Graphical settings, so the Phase 1 Settings Menu is reachable from the graphical main menu.
 *
 * <p>Difficulty is the model's, same as SettingsMenuController: the select box only stops an
 * out-of-range value from being offered. Speed, the grid overlay and debug mode are the graphical
 * build's own, so they live in {@link GameSettings} and are not saved to the account.
 */
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
    panel.add(difficulty).width(200f).height(52f).row();

    SelectBox<String> speed = new SelectBox<>(skin);
    speed.setItems(SPEEDS);
    speed.setSelected(String.valueOf(GameSettings.getGameSpeed()));
    panel.add(new Label("Game speed", skin, UiSkinProvider.LABEL_MEDIUM)).right().padRight(14f);
    panel.add(speed).width(200f).height(52f).row();

    panel.add(new Label("Music", skin, UiSkinProvider.LABEL_MEDIUM)).right().padRight(14f);
    panel.add(volumeSlider(GameSettings.getMusicVolume(), GameSettings::setMusicVolume))
        .width(200f).height(52f).row();

    panel.add(new Label("Sound effects", skin, UiSkinProvider.LABEL_MEDIUM))
        .right().padRight(14f);
    panel.add(volumeSlider(GameSettings.getSfxVolume(), volume -> {
      GameSettings.setSfxVolume(volume);
      // Played on release so the player hears the level they just chose.
      GameAudio.getInstance().play(GameAudio.Sfx.CLICK);
    })).width(200f).height(52f).row();

    panel.add(new Label("Mute", skin, UiSkinProvider.LABEL_MEDIUM)).right().padRight(14f);
    panel.add(toggle(GameSettings.isMuted(), () -> {
      GameSettings.setMuted(!GameSettings.isMuted());
      GameAudio.getInstance().refreshVolumes();
    })).width(200f).height(52f).row();

    panel.add(new Label("Grid overlay", skin, UiSkinProvider.LABEL_MEDIUM)).right().padRight(14f);
    panel.add(toggle(GameSettings.isGridVisible(),
        () -> GameSettings.setGridVisible(!GameSettings.isGridVisible())))
        .width(200f).height(52f).row();

    panel.add(new Label("Debug mode", skin, UiSkinProvider.LABEL_MEDIUM)).right().padRight(14f);
    panel.add(toggle(GameSettings.isDebugMode(),
        () -> GameSettings.setDebugMode(!GameSettings.isDebugMode())))
        .width(200f).height(52f).row();

    panel.add(button("Apply", UiSkinProvider.BUTTON_GREEN, () -> apply(user, difficulty, speed)))
        .colspan(2)
        .width(260f)
        .padTop(14f)
        .row();
    panel.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())))
        .colspan(2)
        .width(260f)
        .row();
    content.add(panel);
  }

  /**
   * A 0..1 slider that applies as it is dragged, so the change is audible while moving it.
   *
   * <p>Unlike difficulty and speed this is not behind Apply: a volume you cannot hear until you
   * commit it is not a volume control.
   */
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

  // Re-entering the screen is what makes the change show: the debug panel is built in show().
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
    try {
      UserManager.getInstance().updateCurrentUserGameState();
      toast("Difficulty " + difficulty.getSelected() + ", speed x" + speed.getSelected() + ".");
    } catch (Exception e) {
      toast(e.getMessage());
    }
  }
}
