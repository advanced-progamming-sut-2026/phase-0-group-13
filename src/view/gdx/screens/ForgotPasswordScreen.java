package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import data.persistence.UserManager;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.UiSkinProvider;


/**
 * Graphical password recovery.
 *
 * <p>Phase 1 recovery is three calls in order — initiatePasswordRecovery, verifyRecoveryAnswer,
 * resetPasswordAfterRecovery — with the recovery user held in UserManager between them. That is
 * still exactly what happens; this screen just shows one step at a time instead of asking the user
 * to type three commands. Nothing about who may reset what is decided here.
 *
 * <p>A wrong answer drops the recovery session in UserManager, so the screen goes back to step one
 * to match, the same way SignInMenuController sends the user back to the Sign In menu.
 */
public final class ForgotPasswordScreen extends MenuScreen {

  private enum Step {
    IDENTIFY,
    ANSWER,
    RESET
  }

  private Table content;
  private Step step = Step.IDENTIFY;
  private String questionText = "";

  public ForgotPasswordScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  protected String title() {
    return "Forgot Password";
  }

  @Override
  protected Screen backTarget() {
    return new LoginScreen(game);
  }

  @Override
  protected void buildContent(Table content) {
    this.content = content;
    refresh();
  }

  /** Rebuilds the panel for the current step. Cheap, and it keeps the step logic in one place. */
  private void refresh() {
    content.clear();
    Table form = panel();

    switch (step) {
      case IDENTIFY -> buildIdentify(form);
      case ANSWER -> buildAnswer(form);
      case RESET -> buildReset(form);
      default -> throw new IllegalStateException("unhandled step " + step);
    }

    form.add(button("Back to Login", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())))
        .colspan(2)
        .width(260f)
        .padTop(10f)
        .row();
    content.add(form);
  }

  private void buildIdentify(Table form) {
    TextField username = field(form, "Username", false);
    TextField email = field(form, "Email", false);
    form.add(button("Continue", UiSkinProvider.BUTTON_GREEN, () -> identify(username, email)))
        .colspan(2)
        .width(260f)
        .padTop(14f)
        .row();
  }

  private void buildAnswer(Table form) {
    form.add(new Label(questionText, skin, UiSkinProvider.LABEL_MEDIUM))
        .colspan(2)
        .padBottom(12f)
        .row();
    TextField answer = field(form, "Answer", false);
    form.add(button("Verify", UiSkinProvider.BUTTON_GREEN, () -> verify(answer)))
        .colspan(2)
        .width(260f)
        .padTop(14f)
        .row();
  }

  private void buildReset(Table form) {
    form.add(new Label("Answer accepted. Choose a new password.", skin,
            UiSkinProvider.LABEL_MEDIUM))
        .colspan(2)
        .padBottom(12f)
        .row();
    TextField newPassword = field(form, "New Password", true);
    form.add(button("Set Password", UiSkinProvider.BUTTON_GREEN, () -> reset(newPassword)))
        .colspan(2)
        .width(260f)
        .padTop(14f)
        .row();
  }

  private void identify(TextField username, TextField email) {
    try {
      questionText =
          UserManager.getInstance()
              .initiatePasswordRecovery(username.getText().trim(), email.getText().trim());
      step = Step.ANSWER;
      refresh();
    } catch (Exception e) {
      toast(e.getMessage());
    }
  }

  private void verify(TextField answer) {
    try {
      UserManager.getInstance().verifyRecoveryAnswer(answer.getText().trim());
      step = Step.RESET;
      refresh();
    } catch (Exception e) {
      // UserManager has dropped the recovery session, so the screen has to start over too.
      step = Step.IDENTIFY;
      refresh();
      toast(e.getMessage());
    }
  }

  private void reset(TextField newPassword) {
    try {
      UserManager.getInstance().resetPasswordAfterRecovery(newPassword.getText());
      go(new LoginScreen(game).withNotice("Password reset. Log in with the new one."));
    } catch (Exception e) {
      toast(e.getMessage());
    }
  }
}
