package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import data.persistence.UserManager;
import model.account.User;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.UiSkinProvider;


/**
 * Graphical profile, so the Phase 1 Profile Menu is reachable from the graphical main menu.
 *
 * <p>Shows what "show info" shows and offers the two rename commands, both through the existing
 * UserManager methods, which are the ones that validate and save. The password and email changes
 * are still terminal-only; nothing about them changed.
 */
public final class ProfileScreen extends MenuScreen {

  public ProfileScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  protected String title() {
    return "Profile";
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

    info(panel, "Username", user.getUsername());
    info(panel, "Nickname", user.getNickname());
    info(panel, "Email", user.getEmail());
    info(panel, "Gender", user.getGender());
    info(panel, "Difficulty", String.valueOf(user.getDifficultyLevel()));

    TextField newUsername = field(panel, "New Username", false);
    panel.add(button("Change Username", UiSkinProvider.BUTTON_GREEN,
            () -> changeUsername(newUsername)))
        .colspan(2)
        .width(280f)
        .padTop(6f)
        .row();

    TextField newNickname = field(panel, "New Nickname", false);
    panel.add(button("Change Nickname", UiSkinProvider.BUTTON_GREEN,
            () -> changeNickname(newNickname)))
        .colspan(2)
        .width(280f)
        .padTop(6f)
        .row();

    panel.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())))
        .colspan(2)
        .width(280f)
        .padTop(14f)
        .row();
    content.add(panel);
  }

  private void info(Table panel, String label, String value) {
    panel.add(new Label(label, skin, "secondary")).right().padRight(14f);
    panel.add(new Label(value, skin, UiSkinProvider.LABEL_MEDIUM)).left().row();
  }

  private void changeUsername(TextField input) {
    try {
      UserManager.getInstance().changeUsername(input.getText().trim());
      go(new ProfileScreen(game).withNotice("Username changed."));
    } catch (Exception e) {
      toast(e.getMessage());
    }
  }

  private void changeNickname(TextField input) {
    try {
      UserManager.getInstance().changeNickname(input.getText().trim());
      go(new ProfileScreen(game).withNotice("Nickname changed."));
    } catch (Exception e) {
      toast(e.getMessage());
    }
  }
}
