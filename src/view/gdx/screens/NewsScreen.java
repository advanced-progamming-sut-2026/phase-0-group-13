package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import data.persistence.UserManager;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import model.account.User;
import model.game.news.AllNews;
import model.game.news.News;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.UiSkinProvider;


/**
 * Graphical news inbox, the two views NewsMenuController offers.
 *
 * <p>Unread does what "show unread" does, including marking the inbox read on the way out and
 * saving, because that is the Phase 1 rule for opening it. All does what "show all" does: newest
 * first regardless of read state, unread ones tagged.
 *
 * <p>Same AllNews instance the terminal build prints from, so the badge on the main menu agrees
 * with what happened here.
 */
public final class NewsScreen extends MenuScreen {

  private Table list;

  public NewsScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  protected String title() {
    return "News";
  }

  @Override
  protected Screen backTarget() {
    return new MainMenuScreen(game);
  }

  @Override
  protected void buildContent(Table content) {
    Table actions = new Table();
    actions.defaults().pad(6f).width(220f);
    actions.add(button("Unread", UiSkinProvider.BUTTON_GREEN, this::showUnread));
    actions.add(button("All", UiSkinProvider.BUTTON_GREEN, this::showAll));
    actions.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())));
    content.add(actions).row();

    list = panel();
    list.top();
    ScrollPane scroll = new ScrollPane(list, skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    content.add(scroll).grow().row();

    showUnread();
  }

  private void showUnread() {
    User user = requireUser();
    if (user == null) {
      return;
    }

    AllNews newsBox = user.getNewsBox();
    List<News> unread = new ArrayList<>(newsBox.getUnreadNews());
    if (unread.isEmpty()) {
      render("You have no unread news.", List.of(), false);
      return;
    }
    render("Unread News (" + unread.size() + ")", unread, false);

    // Opening the inbox marks everything in it as read, same as the terminal menu.
    newsBox.markAllAsRead();
    saveState();
  }

  private void showAll() {
    User user = requireUser();
    if (user == null) {
      return;
    }

    AllNews newsBox = user.getNewsBox();
    List<News> all = new ArrayList<>(newsBox.getUnreadNews());
    all.addAll(newsBox.getReadNews());
    all.sort((first, second) -> Long.compare(second.getTimestamp(), first.getTimestamp()));

    if (all.isEmpty()) {
      render("You have no news yet.", List.of(), false);
      return;
    }
    render("All News (" + all.size() + ")", all, true);
  }

  private void render(String heading, List<News> items, boolean tagUnread) {
    list.clear();
    list.add(new Label(heading, skin, UiSkinProvider.LABEL_MEDIUM)).left().padBottom(10f).row();
    for (News news : items) {
      String tag = tagUnread && !news.isRead() ? "  [UNREAD]" : "";
      list.add(new Label(
              dateOf(news) + "  [" + news.getType() + "] " + news.getMessage() + tag, skin))
          .left()
          .row();
    }
  }

  private static String dateOf(News news) {
    return DATE_FORMAT.format(
        Instant.ofEpochMilli(news.getTimestamp()).atZone(ZoneId.systemDefault()));
  }

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private User requireUser() {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      toast("error: no user logged in");
    }
    return user;
  }

  private void saveState() {
    runAsync(
        () -> {
          UserManager.getInstance().updateCurrentUserGameState();
          return null;
        },
        ignored -> {},
        e -> toast(e.getMessage()));
  }
}
