package view.gdx.screens;

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

  private final HudArt hudArt = new HudArt();

  /**
   * PvZ's own buttons for these destinations, used here as the icon on ours.
   *
   * <p>The greenhouse takes the Zen Garden's watering can because the doc's greenhouse is that
   * feature; the Travel Log takes the quest log; Profile takes the edit button, which is what a
   * profile screen is for. Leaderboard has no skin icon, so it takes the Arena's gold cup out of
   * the HUD sheet -- the same trophy its own top row wears.
   */
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
  protected void buildContent(Table content) {
    User user = UserManager.getInstance().getCurrentUser();
    boolean loggedIn = user != null;

    // The buttons sit on a panel rather than straight on the art: PLAY is the one thing on this
    // screen that has to be found instantly, and a block of buttons floating on a busy lawn reads
    // as a screenshot with controls pasted over it.
    Table menu = panel();
    menu.pad(24f, 40f, 28f, 40f);
    menu.add(new Label(greeting(user), skin, UiSkinProvider.LABEL_MEDIUM)).padBottom(18f).row();

    if (loggedIn) {
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
      // The daily scored run: the only thing that fills My Point on the leaderboard.
      menu.add(button("BONUS GAME", UiSkinProvider.BUTTON_BROWN,
              () -> go(PlantSelectionScreen.forBonusGame(game))))
          .width(420f)
          .height(64f)
          .padBottom(18f)
          .row();
    } else {
      Table entry = new Table();
      entry.defaults().pad(8f).width(200f).height(72f);
      entry.add(button("Login", UiSkinProvider.BUTTON_GREEN, () -> go(new LoginScreen(game))));
      entry.add(button("Sign Up", UiSkinProvider.BUTTON_BROWN, () -> go(new SignUpScreen(game))));
      menu.add(entry).padBottom(24f).row();
    }

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

  /**
   * A button that is only wired up when there's a user, because the Phase 1 sub-menus all start by
   * rejecting a null one. A disabled Button still fires listeners we add ourselves, so the way to
   * make it inert is to not add one.
   */
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

  /**
   * Puts the game's own icon for a destination on the left of its button.
   *
   * <p>These are the buttons PvZ itself uses to reach these screens -- the almanac's book, the Zen
   * Garden's watering can, the quest log, the settings wrench -- so the row says where each button
   * goes before the label is read. The icon is inserted ahead of the label the TextButton already
   * built, which is why its children are rebuilt rather than appended to.
   */
  private TextButton withIcon(TextButton button, String icon) {
    if (icon == null || !skin.has(icon, TextureRegion.class)) {
      return button;
    }
    return withIcon(button, skin.getRegion(icon));
  }

  /** Same, for art that lives in the HUD sheet rather than the skin. */
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
