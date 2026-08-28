package view.gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import network.client.ClientSession;
import network.protocol.Payloads;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.HudArt;
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

  /**
   * The game's own first/second/third place trophies.
   *
   * <p>These are the Arena's league cups, the only actual placement art in the library. They read
   * as a ranking on sight in a way a numbered rosette does not, so the top three carry the cup
   * instead of a number -- gold, silver and bronze say which place it is by themselves.
   */
  private static final String[] MEDALS = {"cupgold", "cupsilver", "cupbronze"};

  /** Cup size in a row. Tall and narrow, like the art. */
  private static final float CUP_WIDTH = 30f;
  private static final float CUP_HEIGHT = 58f;

  private static final Color ME_ROW = new Color(0.16f, 0.52f, 0.18f, 0.55f);
  /** A warm wash behind the podium places, strongest at the top. */
  private static final Color[] PODIUM_ROW = {
      new Color(1f, 0.82f, 0.25f, 0.26f),
      new Color(0.85f, 0.87f, 0.92f, 0.20f),
      new Color(0.80f, 0.53f, 0.28f, 0.18f),
  };
  private static final Color STRIPE_ROW = new Color(0f, 0f, 0f, 0.07f);
  private static final Color CLEAR_ROW = new Color(0f, 0f, 0f, 0f);

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

  private final HudArt hudArt = new HudArt();
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
      board.add(emptyState()).pad(24f).growX();
      return;
    }

    List<Payloads.LeaderboardEntry> ranked = new ArrayList<>(entries);
    if (sortedBy.ascending != null) {
      Comparator<Payloads.LeaderboardEntry> order =
          ascending ? sortedBy.ascending : sortedBy.ascending.reversed();
      ranked.sort(order.thenComparing(Payloads.LeaderboardEntry::username,
          String.CASE_INSENSITIVE_ORDER));
    }

    Table headings = new Table();
    for (Column column : Column.values()) {
      headings.add(heading(column)).width(widthOf(column))
          .align(column == Column.PLAYER ? Align.left : Align.center).pad(6f);
    }
    board.add(headings).growX().row();

    String me = ClientSession.getInstance().getUsername();
    int rank = 1;
    for (Payloads.LeaderboardEntry entry : ranked) {
      boolean isMe = me != null && me.equalsIgnoreCase(entry.username());
      board.add(row(entry, rank, isMe)).growX().padBottom(2f).row();
      rank++;
    }
    // Keeps a short table's rows at the top of the panel instead of centred in a sea of cream.
    board.add().expandY().row();
  }

  /**
   * One player's row.
   *
   * <p>Rows alternate a faint wash so the eye can follow a line across seven columns, and the
   * signed-in player's row is a solid highlight rather than only a brighter font -- on a long
   * table the old treatment was almost invisible. The top three carry the game's own place badges
   * in the rank column, which is what makes the top of the table look like a ranking rather than
   * like row one of a spreadsheet.
   */
  private Table row(Payloads.LeaderboardEntry entry, int rank, boolean isMe) {
    Table row = new Table();
    row.setBackground(skin.newDrawable(UiSkinProvider.WHITE_PIXEL, rowTint(rank, isMe)));
    String style = isMe ? UiSkinProvider.LABEL_MEDIUM : "secondary";

    row.add(rankCell(rank, style)).width(widthOf(Column.RANK)).pad(4f);
    row.add(new Label(entry.username() + (isMe ? "  (you)" : ""), skin, style))
        .width(widthOf(Column.PLAYER)).left().pad(4f);
    cell(row, progressOf(entry), style, Column.PROGRESS);
    cell(row, String.valueOf(entry.miniGameLevels()), style, Column.MINI_GAMES);
    cell(row, String.valueOf(entry.dailyQuests()), style, Column.DAILY);
    cell(row, String.valueOf(entry.otherQuests()), style, Column.OTHER);
    // Blank, not zero: the doc says a player who has not played the bonus game has no My Point.
    cell(row, entry.myPoint() == null ? "-" : String.valueOf(entry.myPoint()), style,
        Column.MY_POINT);
    return row;
  }

  private void cell(Table row, String text, String style, Column column) {
    Label label = new Label(text, skin, style);
    label.setAlignment(Align.center);
    row.add(label).width(widthOf(column)).pad(4f);
  }

  /**
   * Your own row wins over everything; then the podium wash; then the ordinary stripe.
   *
   * <p>Deliberately in that order: finding yourself is the thing a player does first, so it must
   * not be outranked by being third.
   */
  private static Color rowTint(int rank, boolean isMe) {
    if (isMe) {
      return ME_ROW;
    }
    if (rank <= PODIUM_ROW.length) {
      return PODIUM_ROW[rank - 1];
    }
    return rank % 2 == 0 ? STRIPE_ROW : CLEAR_ROW;
  }

  /** A trophy for the top three, the plain number for everyone else. */
  private Table rankCell(int rank, String style) {
    Table cell = new Table();
    TextureRegion cup = rank <= MEDALS.length ? hudArt.find(MEDALS[rank - 1]) : null;
    if (cup != null) {
      Image trophy = new Image(cup);
      trophy.setScaling(Scaling.fit);
      cell.add(trophy).size(CUP_WIDTH, CUP_HEIGHT);
      return cell;
    }
    cell.add(new Label(String.valueOf(rank), skin, style));
    return cell;
  }

  /** Loading and "nobody here" are different things, so they do not look the same. */
  private Table emptyState() {
    Table empty = new Table();
    boolean loading = entries == null && status != null && status.startsWith("Asking");
    empty.add(new Label(loading ? "..." : "!", skin, UiSkinProvider.LABEL_BIG)).padBottom(8f).row();
    Label message = new Label(status == null ? "" : status, skin, UiSkinProvider.LABEL_MEDIUM);
    message.setAlignment(Align.center);
    message.setWrap(true);
    empty.add(message).width(520f).row();
    return empty;
  }

  private static float widthOf(Column column) {
    return column == Column.PLAYER ? 220f : 132f;
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
    hudArt.dispose();
    super.dispose();
  }
}
