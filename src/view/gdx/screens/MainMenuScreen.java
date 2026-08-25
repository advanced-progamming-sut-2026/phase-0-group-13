package view.gdx.screens;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import data.persistence.UserManager;
import model.account.User;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.UiSkinProvider;


/**
 * The graphical main menu, matching the Phase 1 one.
 *
 * <p>Logged out it offers Login and Sign Up and greys the rest out, which is the same rule the
 * terminal build enforces by refusing the sub-menu commands with "no user logged in". Logged in it
 * is the hub the other screens come back to.
 *
 * <p>The unread badge reads the same AllNews the terminal MainMenuView prints its "[NEWS *]" from.
 * Rebuilt on every show(), so logging in or reading the news updates it without any listeners.
 */
public final class MainMenuScreen extends MenuScreen {

  public MainMenuScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  protected String title() {
    return "Plants vs. Zombies";
  }

  @Override
  protected String backgroundAtlasPath() {
    return "textures/environment/darkagesseason.atlas";
  }

  @Override
  protected void buildContent(Table content) {
    User user = UserManager.getInstance().getCurrentUser();
    boolean loggedIn = user != null;

    // No panel here, the buttons sit straight on the background art.
    Table menu = new Table();
    menu.add(new Label(greeting(user), skin, UiSkinProvider.LABEL_MEDIUM)).padBottom(22f).row();

    if (loggedIn) {
      menu.add(button("PLAY", UiSkinProvider.BUTTON_GREEN, this::play))
          .width(420f)
          .height(96f)
          .padBottom(24f)
          .row();
    } else {
      Table entry = new Table();
      entry.defaults().pad(8f).width(200f).height(72f);
      entry.add(button("Login", UiSkinProvider.BUTTON_GREEN, () -> go(new LoginScreen(game))));
      entry.add(button("Sign Up", UiSkinProvider.BUTTON_BROWN, () -> go(new SignUpScreen(game))));
      menu.add(entry).padBottom(24f).row();
    }

    Table shortcuts = new Table();
    shortcuts.defaults().pad(6f).width(190f).height(64f);
    shortcuts.add(gated(newsLabel(user), () -> go(new NewsScreen(game)), loggedIn));
    shortcuts.add(gated("Collection", () -> go(new CollectionScreen(game)), loggedIn));
    shortcuts.add(gated("Greenhouse", () -> go(new GreenhouseScreen(game)), loggedIn));
    shortcuts.add(gated("Shop", () -> go(new ShopScreen(game)), loggedIn));
    menu.add(shortcuts).row();

    Table shortcuts2 = new Table();
    shortcuts2.defaults().pad(6f).width(190f).height(64f);
    shortcuts2.add(gated("Travel Log", () -> go(new QuestScreen(game)), loggedIn));
    // The doc asks for the leaderboard to be reachable from the main menu.
    shortcuts2.add(gated("Leaderboard", () -> go(new LeaderboardScreen(game)), loggedIn));
    shortcuts2.add(gated("Profile", () -> go(new ProfileScreen(game)), loggedIn));
    shortcuts2.add(gated("Settings", () -> go(new SettingsScreen(game)), loggedIn));
    menu.add(shortcuts2).row();

    if (loggedIn) {
      menu.add(button("Logout", UiSkinProvider.BUTTON_BROWN, this::logout))
          .width(190f)
          .padTop(18f)
          .row();
    }

    content.add(menu);
  }

  private String greeting(User user) {
    return user == null
        ? "Not logged in - log in or create an account to play."
        : "Welcome back, " + user.getUsername() + "!";
  }

  private String newsLabel(User user) {
    int unread = user == null ? 0 : user.getNewsBox().getUnreadCount();
    return unread > 0 ? "News (" + unread + ") !" : "News";
  }

  /**
   * A button that is only wired up when there's a user, because the Phase 1 sub-menus all start by
   * rejecting a null one. A disabled Button still fires listeners we add ourselves, so the way to
   * make it inert is to not add one.
   */
  private TextButton gated(String text, Runnable action, boolean enabled) {
    if (!enabled) {
      TextButton button = new TextButton(text, skin, UiSkinProvider.BUTTON_GREEN);
      button.setDisabled(true);
      return button;
    }
    return button(text, UiSkinProvider.BUTTON_GREEN, action);
  }

  private void play() {
    go(new AdventureScreen(game));
  }

  private void logout() {
    UserManager.getInstance().logout();
    go(new MainMenuScreen(game));
  }
}
