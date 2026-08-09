package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import data.persistence.UserManager;
import model.account.User;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.UiSkinProvider;


/**
 * Graphical settings, so the Phase 1 Settings Menu is reachable from the graphical main menu.
 *
 * <p>Difficulty is the one thing that menu changes. The range and the save are the model's, same
 * as SettingsMenuController: the select box only stops an out-of-range value from being offered.
 */
public final class SettingsScreen extends MenuScreen {

  private static final String[] LEVELS = {"1", "2", "3", "4", "5"};

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

    panel.add(button("Apply", UiSkinProvider.BUTTON_GREEN, () -> apply(user, difficulty)))
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

  private void apply(User user, SelectBox<String> difficulty) {
    user.setDifficultyLevel(Integer.parseInt(difficulty.getSelected()));
    try {
      UserManager.getInstance().updateCurrentUserGameState();
      toast("Difficulty level changed to " + difficulty.getSelected() + ".");
    } catch (Exception e) {
      toast(e.getMessage());
    }
  }
}
