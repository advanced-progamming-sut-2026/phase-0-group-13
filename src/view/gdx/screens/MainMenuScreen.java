package view.gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Scaling;
import data.persistence.UserManager;
import model.account.User;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.HudArt;
import view.gdx.ui.Popup;
import view.gdx.ui.UiSkinProvider;


public final class MainMenuScreen extends MenuScreen {

  private final HudArt hudArt = new HudArt();

  private static final String ICON_NEWS = "image_ui_hud_tasklist_buttons_hud_task_list_normal";
  private static final String ICON_ALMANAC =
      "image_ui_hud_almanacbutton_buttons_hud_almanac_normal";
  private static final String ICON_GARDEN = "image_ui_generic_buttons_hud_zg_normal";
  private static final String ICON_SHOP = "image_ui_hud_eventshop_buttons_hud_event_shop_normal";
  private static final String ICON_QUESTS = "image_ui_generic_buttons_hud_quests_normal";
  private static final String ICON_PROFILE = "image_ui_mainmenu_edit_btn_normal";
  private static final String ICON_SETTINGS =
      "image_ui_hud_settingsbutton_buttons_hud_settings_normal";

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
  protected void onEscape() {
    Popup.show(stage, skin, "Quit the game?", null,
        new Popup.Choice("Quit", UiSkinProvider.BUTTON_BROWN, () -> Gdx.app.exit()),
        new Popup.Choice("Keep playing", UiSkinProvider.BUTTON_GREEN, null));
  }

  /** The three big buttons a signed-in player gets at the top of the menu. */
  private void addPlayButtons(Table menu) {
    menu.add(button("PLAY", UiSkinProvider.BUTTON_GREEN, this::play))
        .width(420f)
        .height(86f)
        .padBottom(10f)
        .row();
    menu.add(button("MULTIPLAYER", UiSkinProvider.BUTTON_PURPLE,
            () -> go(new MultiplayerScreen(game))))
        .width(420f)
        .height(64f)
        .padBottom(10f)
        .row();
    menu.add(button("BONUS GAME", UiSkinProvider.BUTTON_BROWN,
            () -> go(PlantSelectionScreen.forBonusGame(game))))
        .width(420f)
        .height(64f)
        .padBottom(18f)
        .row();
  }

  /** The two rows of smaller destinations, greyed out until someone is signed in. */
  private void addShortcutRows(Table menu, User user, boolean loggedIn) {
    Table shortcuts = new Table();
    shortcuts.defaults().pad(5f).width(186f).height(58f);
    shortcuts.add(gated(newsLabel(user), ICON_NEWS, () -> go(new NewsScreen(game)), loggedIn));
    shortcuts.add(gated("Collection", ICON_ALMANAC,
        () -> go(new CollectionScreen(game)), loggedIn));
    shortcuts.add(gated("Greenhouse", ICON_GARDEN,
        () -> go(new GreenhouseScreen(game)), loggedIn));
    shortcuts.add(gated("Shop", ICON_SHOP, () -> go(new ShopScreen(game)), loggedIn));
    menu.add(shortcuts).row();

    Table shortcuts2 = new Table();
    shortcuts2.defaults().pad(5f).width(186f).height(58f);
    shortcuts2.add(gated("Travel Log", ICON_QUESTS, () -> go(new QuestScreen(game)), loggedIn));
    // The doc asks for the leaderboard to be reachable from the main menu.
    shortcuts2.add(withIcon(gated("Leaderboard", null,
        () -> go(new LeaderboardScreen(game)), loggedIn), hudArt.find("cupgold")));
    shortcuts2.add(gated("Profile", ICON_PROFILE, () -> go(new ProfileScreen(game)), loggedIn));
    shortcuts2.add(gated("Settings", ICON_SETTINGS,
        () -> go(new SettingsScreen(game)), loggedIn));
    menu.add(shortcuts2).row();
  }

  @Override
  protected void buildContent(Table content) {
    User user = UserManager.getInstance().getCurrentUser();
    boolean loggedIn = user != null;

    Table menu = panel();
    menu.pad(24f, 40f, 28f, 40f);
    menu.add(new Label(greeting(user), skin, UiSkinProvider.LABEL_MEDIUM)).padBottom(18f).row();

    if (loggedIn) {
      addPlayButtons(menu);
    } else {
      Table entry = new Table();
      entry.defaults().pad(8f).width(200f).height(72f);
      entry.add(button("Login", UiSkinProvider.BUTTON_GREEN, () -> go(new LoginScreen(game))));
      entry.add(button("Sign Up", UiSkinProvider.BUTTON_BROWN, () -> go(new SignUpScreen(game))));
      menu.add(entry).padBottom(24f).row();
    }

    addShortcutRows(menu, user, loggedIn);

    if (loggedIn) {
      menu.add(button("Logout", UiSkinProvider.BUTTON_BROWN, this::logout))
          .width(190f)
          .height(52f)
          .padTop(14f)
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

  private TextButton gated(String text, String icon, Runnable action, boolean enabled) {
    TextButton button;
    if (!enabled) {
      button = new TextButton(text, skin, UiSkinProvider.BUTTON_GREEN);
      button.setDisabled(true);
    } else {
      button = button(text, UiSkinProvider.BUTTON_GREEN, action);
    }
    return withIcon(button, icon);
  }

  private TextButton withIcon(TextButton button, String icon) {
    if (icon == null || !skin.has(icon, TextureRegion.class)) {
      return button;
    }
    return withIcon(button, skin.getRegion(icon));
  }

  private TextButton withIcon(TextButton button, TextureRegion icon) {
    if (icon == null) {
      return button;
    }
    Label label = button.getLabel();
    button.clearChildren();
    Image image = new Image(icon);
    image.setScaling(Scaling.fit);
    button.add(image).size(30f).padRight(7f);
    button.add(label);
    return button;
  }

  private void play() {
    go(new AdventureScreen(game));
  }

  @Override
  public void dispose() {
    hudArt.dispose();
    super.dispose();
  }

  private void logout() {
    UserManager.getInstance().logout();
    go(new MainMenuScreen(game));
  }
}
