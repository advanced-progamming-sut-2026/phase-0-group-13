package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import data.persistence.UserManager;
import model.account.AdventureMap;
import model.account.User;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.UiSkinProvider;


/**
 * Graphical profile, so the Phase 1 Profile Menu is reachable from the graphical main menu.
 *
 * <p>Shows what "show info" shows and offers the same four changes the typed menu does, all through
 * the existing UserManager methods, which are the ones that validate and save.
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
    info(panel, "Games played", String.valueOf(user.getRecentGames().size()));
    info(panel, "Stages completed", String.valueOf(completedStages(user)));
    info(panel, "Highest My-Point", String.valueOf(user.getMeowPoints()));

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

    TextField newEmail = field(panel, "New Email", false);
    panel.add(button("Change Email", UiSkinProvider.BUTTON_GREEN, () -> changeEmail(newEmail)))
        .colspan(2)
        .width(280f)
        .padTop(6f)
        .row();

    TextField oldPassword = field(panel, "Current Password", true);
    TextField newPassword = field(panel, "New Password", true);
    panel.add(button("Change Password", UiSkinProvider.BUTTON_GREEN,
            () -> changePassword(newPassword, oldPassword)))
        .colspan(2)
        .width(280f)
        .padTop(6f)
        .row();

    panel.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())))
        .colspan(2)
        .width(280f)
        .padTop(14f)
        .row();

    // Too many rows for a short window now, so it scrolls like the sign-up form does.
    ScrollPane scroll = new ScrollPane(panel, skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    content.add(scroll).grow();
  }

  /** Same count "show info" prints: four levels a stage, plus however far into the current one. */
  private static int completedStages(User user) {
    return Math.max(0, (user.getProgress().getMaxClearedStage() - 1) * AdventureMap.LEVELS_PER_STAGE
        + user.getProgress().getMaxClearedLevel());
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

  private void changeEmail(TextField input) {
    try {
      UserManager.getInstance().changeEmail(input.getText().trim());
      go(new ProfileScreen(game).withNotice("Email changed."));
    } catch (Exception e) {
      toast(e.getMessage());
    }
  }

  private void changePassword(TextField newPassword, TextField oldPassword) {
    try {
      UserManager.getInstance().changePassword(newPassword.getText(), oldPassword.getText().trim());
      go(new ProfileScreen(game).withNotice("Password changed."));
    } catch (Exception e) {
      toast(e.getMessage());
    }
  }
}
