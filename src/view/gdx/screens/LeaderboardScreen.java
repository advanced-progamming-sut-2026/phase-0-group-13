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

/** The leaderboard, as the doc's mandatory menu. */
public final class LeaderboardScreen extends MenuScreen {

  /** 0 asks for every registered player, which is what the doc's table is. */
  private static final int NO_LIMIT = 0;

  private static final String[] MEDALS = {"cupgold", "cupsilver", "cupbronze"};

  private static final float CUP_WIDTH = 30f;
  private static final float CUP_HEIGHT = 58f;

  private static final Color ME_ROW = new Color(0.16f, 0.52f, 0.18f, 0.55f);
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
    board.add().expandY().row();
  }

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

  private static Color rowTint(int rank, boolean isMe) {
    if (isMe) {
      return ME_ROW;
    }
    if (rank <= PODIUM_ROW.length) {
      return PODIUM_ROW[rank - 1];
    }
    return rank % 2 == 0 ? STRIPE_ROW : CLEAR_ROW;
  }

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
        ascending = column == sortedBy && !ascending;
        sortedBy = column;
        redraw();
      }
    });
    return label;
  }

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
