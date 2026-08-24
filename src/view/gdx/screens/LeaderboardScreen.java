package view.gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import network.client.ClientSession;
import network.protocol.Payloads;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.UiSkinProvider;

/**
 * The leaderboard, as the doc's mandatory menu.
 *
 * <p>Every row comes from the server: the screen asks LEADERBOARD_REQUEST and draws the reply. It
 * deliberately does not read {@code UserManager.getAllUsers()} the way the terminal board does --
 * that is this machine's mirror of the accounts, and Phase Three says the table is built from
 * server-stored user data. So a player who has never signed in on this computer still appears, and
 * a local file that has drifted cannot change the standings.
 *
 * <p>The request runs on its own thread because the protocol call blocks on the socket, and
 * blocking here would freeze the render loop for as long as the server took to answer. The result
 * comes back through {@code Gdx.app.postRunnable}, which is the only safe way to touch the stage
 * from off the render thread. That gives the screen three states to show -- asking, failed, and a
 * table -- rather than a window that stops repainting.
 *
 * <p>Columns are the ones the doc lists, and clicking a heading sorts by it: first click descending
 * (the useful direction for a ranking), clicking the same heading again flips to ascending.
 */
public final class LeaderboardScreen extends MenuScreen {

  /** 0 asks for every registered player, which is what the doc's table is. */
  private static final int NO_LIMIT = 0;

  private enum Column {
    RANK("#", null),
    PLAYER("Player", Comparator.comparing(Payloads.LeaderboardEntry::username,
        String.CASE_INSENSITIVE_ORDER)),
    PROGRESS("Last cleared", Comparator
        .comparingInt(Payloads.LeaderboardEntry::lastSeason)
        .thenComparingInt(Payloads.LeaderboardEntry::lastStage)),
    MINI_GAMES("Mini-games", Comparator.comparingInt(Payloads.LeaderboardEntry::miniGameLevels)),
    DAILY("Daily quests", Comparator.comparingInt(Payloads.LeaderboardEntry::dailyQuests)),
    OTHER("Other quests", Comparator.comparingInt(Payloads.LeaderboardEntry::otherQuests)),
    MY_POINT("My Point", Comparator.comparing(
        e -> e.myPoint() == null ? Integer.MIN_VALUE : e.myPoint()));

    private final String heading;
    private final Comparator<Payloads.LeaderboardEntry> ascending;

    Column(String heading, Comparator<Payloads.LeaderboardEntry> ascending) {
      this.heading = heading;
      this.ascending = ascending;
    }
  }

  private Table board;
  private List<Payloads.LeaderboardEntry> entries;
  private String status = "Asking the server for the leaderboard...";
  private Column sortedBy = Column.MY_POINT;
  private boolean ascending;

  public LeaderboardScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  protected String title() {
    return "Leaderboard";
  }

  @Override
  protected Screen backTarget() {
    return new MainMenuScreen(game);
  }

  @Override
  protected void buildContent(Table content) {
    Table actions = new Table();
    actions.defaults().pad(6f).width(200f);
    actions.add(button("Refresh", UiSkinProvider.BUTTON_GREEN, this::fetch));
    actions.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())));
    content.add(actions).row();

    board = panel();
    board.top();
    ScrollPane scroll = new ScrollPane(board, skin);
    scroll.setFadeScrollBars(false);
    content.add(scroll).grow().row();

    redraw();
    fetch();
  }

  /** Asks the server off the render thread, then redraws once the answer is in. */
  private void fetch() {
    entries = null;
    status = "Asking the server for the leaderboard...";
    redraw();

    Thread worker = new Thread(() -> {
      List<Payloads.LeaderboardEntry> fetched = null;
      String problem = null;
      ClientSession session = ClientSession.getInstance();
      if (!session.isConnected() && !session.connect()) {
        problem = session.getLastError();
      } else if (!session.isAuthenticated()) {
        // The server only answers a signed-in connection, so say that rather than letting the
        // refusal come back looking like a leaderboard with nobody on it.
        problem = "error: log in to see the leaderboard.";
      } else {
        try {
          fetched = session.requestLeaderboard(NO_LIMIT).entries();
        } catch (IOException e) {
          problem = "error: the server did not answer (" + e.getMessage() + ")";
        }
      }
      List<Payloads.LeaderboardEntry> result = fetched;
      String failure = problem;
      Gdx.app.postRunnable(() -> {
        // The screen can be gone by now if the player pressed Back while this was in flight.
        if (board == null) {
          return;
        }
        if (failure != null) {
          entries = null;
          status = failure;
        } else {
          entries = result == null ? List.of() : result;
          status = entries.isEmpty() ? "No players have registered yet." : null;
        }
        redraw();
      });
    }, "leaderboard-fetch");
    worker.setDaemon(true);
    worker.start();
  }

  private void redraw() {
    if (board == null) {
      return;
    }
    board.clear();
    if (entries == null || entries.isEmpty()) {
      board.add(new Label(status == null ? "" : status, skin, UiSkinProvider.LABEL_MEDIUM))
          .pad(24f);
      return;
    }

    List<Payloads.LeaderboardEntry> ranked = new ArrayList<>(entries);
    if (sortedBy.ascending != null) {
      Comparator<Payloads.LeaderboardEntry> order =
          ascending ? sortedBy.ascending : sortedBy.ascending.reversed();
      ranked.sort(order.thenComparing(Payloads.LeaderboardEntry::username,
          String.CASE_INSENSITIVE_ORDER));
    }

    for (Column column : Column.values()) {
      board.add(heading(column)).pad(6f).minWidth(column == Column.PLAYER ? 190f : 130f);
    }
    board.row();

    String me = ClientSession.getInstance().getUsername();
    int rank = 1;
    for (Payloads.LeaderboardEntry entry : ranked) {
      boolean isMe = me != null && me.equalsIgnoreCase(entry.username());
      // The player's own row is called out so they can find themselves in a long table.
      String style = isMe ? UiSkinProvider.LABEL_MEDIUM : "secondary";
      board.add(new Label(String.valueOf(rank++), skin, style)).pad(6f);
      board.add(new Label(entry.username() + (isMe ? "  (you)" : ""), skin, style)).left().pad(6f);
      board.add(new Label(progressOf(entry), skin, style)).pad(6f);
      board.add(new Label(String.valueOf(entry.miniGameLevels()), skin, style)).pad(6f);
      board.add(new Label(String.valueOf(entry.dailyQuests()), skin, style)).pad(6f);
      board.add(new Label(String.valueOf(entry.otherQuests()), skin, style)).pad(6f);
      // Blank, not zero: the doc says a player who has not played the bonus game has no My Point.
      board.add(new Label(entry.myPoint() == null ? "-" : String.valueOf(entry.myPoint()),
          skin, style)).pad(6f);
      board.row();
    }
  }

  /** A heading that sorts on click, with an arrow showing which way it is sorted. */
  private Label heading(Column column) {
    String text = column.heading;
    if (column == sortedBy && column.ascending != null) {
      text = text + (ascending ? "  ^" : "  v");
    }
    Label label = new Label(text, skin, UiSkinProvider.LABEL_MEDIUM);
    if (column.ascending == null) {
      return label;
    }
    label.addListener(new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        // Clicking the column it is already sorted by flips the direction; a new column starts
        // descending, which is the way round a ranking is normally read.
        ascending = column == sortedBy && !ascending;
        sortedBy = column;
        redraw();
      }
    });
    return label;
  }

  /** "Season 2 Stage 3", or a dash for an account that has not cleared a level yet. */
  private static String progressOf(Payloads.LeaderboardEntry entry) {
    if (entry.lastSeason() <= 0 || entry.lastStage() <= 0) {
      return "-";
    }
    return "Season " + entry.lastSeason() + " Stage " + entry.lastStage();
  }

  @Override
  public void dispose() {
    board = null;
    super.dispose();
  }
}
