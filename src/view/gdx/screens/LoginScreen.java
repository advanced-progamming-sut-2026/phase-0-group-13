package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import data.persistence.UserManager;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.UiSkinProvider;


/**
 * Graphical sign in.
 *
 * <p>Calls UserManager.loginUser, the same method SignInMenuController.handleLogin calls once it
 * has finished picking the arguments out of the typed command. The username lookup, the hash
 * comparison, the quest seeding and the stay-logged-in file all stay where they are; this screen
 * only collects the three values and turns the exception into a toast instead of a println.
 */
public final class LoginScreen extends MenuScreen {

  private TextField username;
  private TextField password;
  private CheckBox stayLoggedIn;

  public LoginScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  protected String title() {
    return "Login";
  }

  @Override
  protected Screen backTarget() {
    return new MainMenuScreen(game);
  }

  @Override
  protected void buildContent(Table content) {
    Table form = panel();
    username = field(form, "Username", false);
    password = field(form, "Password", true);

    stayLoggedIn = new CheckBox(" Stay logged in", skin);
    form.add(stayLoggedIn).colspan(2).left().padTop(4f).row();

    Table actions = new Table();
    actions.defaults().pad(6f).width(200f);
    actions.add(button("Login", UiSkinProvider.BUTTON_GREEN, this::login));
    actions.add(
        button("Forgot Password", UiSkinProvider.BUTTON_PURPLE,
            () -> go(new ForgotPasswordScreen(game))));
    form.add(actions).colspan(2).padTop(14f).row();

    Table nav = new Table();
    nav.defaults().pad(6f).width(200f);
    nav.add(button("Sign Up", UiSkinProvider.BUTTON_BROWN, () -> go(new SignUpScreen(game))));
    nav.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())));
    form.add(nav).colspan(2).row();

    content.add(form);
  }

  private void login() {
    try {
      UserManager.getInstance()
          .loginUser(username.getText().trim(), password.getText(), stayLoggedIn.isChecked());
      go(new MainMenuScreen(game));
    } catch (Exception e) {
      // The screen stays exactly as it was, so the username is still there to correct.
      password.setText("");
      toast(e.getMessage());
    }
  }
}
