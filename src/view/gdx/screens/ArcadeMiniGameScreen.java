package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import data.persistence.UserManager;
import model.account.User;
import model.core.MiniGameLauncher;
import model.enums.MiniGameType;
import model.game.minigame.arcade.BeghouledEngine;
import model.game.minigame.arcade.IZombieEngine;
import model.game.minigame.arcade.VasebreakerEngine;
import model.game.minigame.arcade.WallnutBowlingEngine;
import view.BoardRenderer;
import view.gdx.core.GameSettings;
import view.gdx.core.GdxConfig;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.Popup;
import view.gdx.ui.UiSkinProvider;


/**
 * The four arcade mini-games on a clickable grid, so they are no longer typing-only.
 *
 * <p>One screen for all four because they are all a 5x9 board plus one action: the engines are the
 * Phase 1 ones, the cell text comes from {@link BoardRenderer}'s own cell helpers, and the reward
 * goes through {@link MiniGameLauncher#awardClear}. Nothing about a mini-game is decided here.
 *
 * <p>Zombotany isn't one of these: it's an ordinary stage with plant-zombies, so it plays on
 * GameplayScreen like any other level.
 */
public final class ArcadeMiniGameScreen extends MenuScreen {

  private static final int ROWS = 5;
  private static final int COLS = 9;

  private final MiniGameType type;
  private final int level;
  private final TextButton[][] cells = new TextButton[ROWS][COLS];

  private VasebreakerEngine vasebreaker;
  private WallnutBowlingEngine bowling;
  private IZombieEngine izombie;
  private BeghouledEngine beghouled;

  private Label header;
  private Table sidebar;
  private float accumulator;
  private boolean finished;

  /** What the next cell click will use: a seed, a zombie type, or the first half of a swap. */
  private String chosenSeed;
  private String chosenZombie;
  private int swapRow = -1;
  private int swapColumn = -1;

  public ArcadeMiniGameScreen(PvzGdxGame game, MiniGameType type, int level) {
    super(game);
    this.type = type;
    this.level = level;
    switch (type) {
      case VASEBREAKER -> vasebreaker = new VasebreakerEngine(level);
      case WALLNUT_BOWLING -> bowling = new WallnutBowlingEngine(level);
      case I_ZOMBIE -> izombie = new IZombieEngine(level);
      case BEGHOULED -> beghouled = new BeghouledEngine(level);
      default -> { }
    }
  }

  @Override
  protected String title() {
    return type.name().toLowerCase().replace('_', ' ') + "  -  level " + level;
  }

  @Override
  protected Screen backTarget() {
    return new MiniGamesScreen(game);
  }

  @Override
  protected void buildContent(Table content) {
    header = new Label("", skin, UiSkinProvider.LABEL_MEDIUM);
    content.add(header).left().padBottom(6f).row();

    Table board = new Table();
    board.defaults().pad(2f).width(104f).height(54f);
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        final int cellRow = row;
        final int cellColumn = col;
        TextButton cell = button(".", UiSkinProvider.BUTTON_BROWN,
            () -> click(cellRow, cellColumn));
        cell.getLabel().setFontScale(0.8f);
        cells[row][col] = cell;
        board.add(cell);
      }
      board.row();
    }

    sidebar = new Table();
    sidebar.top();
    Table split = new Table();
    split.add(board).top();
    split.add(sidebar).top().width(280f).padLeft(16f);
    content.add(split).row();

    content.add(button("Give up", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())))
        .width(220f)
        .padTop(8f);
    refresh();
  }

  @Override
  public void render(float delta) {
    step(delta);
    super.render(delta);
  }

  /** Real time into engine ticks, at the same rate the rest of the graphical build steps. */
  private void step(float delta) {
    if (finished || skin == null) {
      return;
    }
    accumulator += delta * GameSettings.getGameSpeed();
    boolean ticked = false;
    while (accumulator >= GdxConfig.SECONDS_PER_TICK) {
      accumulator -= GdxConfig.SECONDS_PER_TICK;
      tick();
      ticked = true;
    }
    if (ticked) {
      refresh();
      checkFinished();
    }
  }

  private void tick() {
    switch (type) {
      case VASEBREAKER -> vasebreaker.tick();
      case WALLNUT_BOWLING -> bowling.tick();
      case I_ZOMBIE -> izombie.tick();
      case BEGHOULED -> beghouled.tick();
      default -> { }
    }
  }

  private void click(int row, int column) {
    if (finished) {
      return;
    }
    String message = switch (type) {
      case VASEBREAKER -> smashOrPlant(row, column);
      case WALLNUT_BOWLING -> bowling.plantNut(row, column);
      case I_ZOMBIE -> chosenZombie == null
          ? "error: pick a zombie on the right first"
          : izombie.placeZombie(chosenZombie, row, column);
      case BEGHOULED -> pickOrSwap(row, column);
      default -> null;
    };
    if (message != null && !message.isBlank()) {
      toast(message);
    }
    refresh();
    checkFinished();
  }

  /** A seed picked off the sidebar gets planted; otherwise the click smashes the vase. */
  private String smashOrPlant(int row, int column) {
    if (chosenSeed == null) {
      return vasebreaker.smash(row, column);
    }
    String seed = chosenSeed;
    chosenSeed = null;
    return vasebreaker.plantSeed(seed, row, column);
  }

  private String pickOrSwap(int row, int column) {
    if (swapRow < 0) {
      swapRow = row;
      swapColumn = column;
      return "picked (" + (column + 1) + ", " + (row + 1) + ") - now click a neighbour";
    }
    String result = beghouled.swap(swapRow, swapColumn, row, column);
    swapRow = -1;
    swapColumn = -1;
    return result;
  }

  private void refresh() {
    header.setText(headerText());
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        cells[row][col].setText(cellText(row, col));
      }
    }
    buildSidebar();
  }

  private String cellText(int row, int column) {
    String[] details = switch (type) {
      case VASEBREAKER -> BoardRenderer.getVaseCellDetails(vasebreaker, row, column);
      case WALLNUT_BOWLING -> BoardRenderer.getBowlingCellDetails(bowling, row, column);
      case I_ZOMBIE -> BoardRenderer.getIZombieCellDetails(izombie, row, column);
      case BEGHOULED -> BoardRenderer.getBeghouledCellDetails(beghouled, row, column);
      default -> new String[] {".", "-"};
    };
    return "-".equals(details[1]) ? details[0] : details[0] + "\n" + details[1];
  }

  private String headerText() {
    return switch (type) {
      case VASEBREAKER -> "zombies loose: " + vasebreaker.getZombies().size()
          + "   -   click a vase to smash it";
      case WALLNUT_BOWLING -> "score " + bowling.getScore() + "   -   left to spawn "
          + bowling.getZombiesRemainingToSpawn() + "   -   on the belt: "
          + bowling.getReadyNutLabel();
      case I_ZOMBIE -> "zombie-sun " + izombie.getZombieSun() + "   -   brains left "
          + izombie.getBrainsRemaining() + "/" + IZombieEngine.BRAINS;
      case BEGHOULED -> "sun " + beghouled.getSun() + "   -   matches "
          + beghouled.getMatchesMade() + "/" + beghouled.getMatchTarget();
      default -> "";
    };
  }

  /** Whatever this mini-game needs picking before a cell click means anything. */
  private void buildSidebar() {
    sidebar.clear();
    sidebar.defaults().pad(3f).width(260f).height(46f);
    switch (type) {
      case VASEBREAKER -> {
        sidebar.add(new Label("seeds on the ground", skin, "secondary")).row();
        for (String seed : vasebreaker.getPendingSeedNames()) {
          sidebar.add(button(seed.equals(chosenSeed) ? seed + " (held)" : seed,
              seed.equals(chosenSeed) ? UiSkinProvider.BUTTON_GREEN : UiSkinProvider.BUTTON_BROWN,
              () -> {
                chosenSeed = seed;
                refresh();
              })).row();
        }
      }
      case I_ZOMBIE -> {
        sidebar.add(new Label("deploy right of the red line", skin, "secondary")).row();
        for (IZombieEngine.ZombieSpec spec : izombie.availableZombieTypes()) {
          sidebar.add(button(spec.name + "  (" + spec.cost + ")",
              spec.name.equals(chosenZombie)
                  ? UiSkinProvider.BUTTON_GREEN : UiSkinProvider.BUTTON_BROWN,
              () -> {
                chosenZombie = spec.name;
                refresh();
              })).row();
        }
      }
      case BEGHOULED -> {
        sidebar.add(new Label("upgrades", skin, "secondary")).row();
        for (BeghouledEngine.Upgrade upgrade : beghouled.getUpgrades()) {
          sidebar.add(button(upgrade.from.label + " -> " + upgrade.to.label
              + "  (" + upgrade.cost + ")", UiSkinProvider.BUTTON_PURPLE, () -> {
                toast(beghouled.upgrade(upgrade.from.label));
                refresh();
              })).row();
        }
      }
      default -> sidebar.add(new Label("plant in columns 1-"
          + (WallnutBowlingEngine.RED_LINE_COLUMN + 1), skin, "secondary")).row();
    }
  }

  private void checkFinished() {
    if (finished || !engineFinished()) {
      return;
    }
    finished = true;
    boolean won = engineWon();
    User user = UserManager.getInstance().getCurrentUser();
    if (won && user != null) {
      MiniGameLauncher.awardClear(user, type, level);
      try {
        UserManager.getInstance().updateCurrentUserGameState();
      } catch (Exception e) {
        toast(e.getMessage());
      }
    }
    Table body = new Table();
    body.add(new Label(won ? "Mini-game cleared!" : "The zombies won this one.",
        skin, UiSkinProvider.LABEL_MEDIUM));
    Popup.show(stage, skin, won ? "You win" : "You lose", body,
        "Back to mini-games", () -> go(backTarget()), null, null);
  }

  private boolean engineFinished() {
    return switch (type) {
      case VASEBREAKER -> vasebreaker.isFinished();
      case WALLNUT_BOWLING -> bowling.isFinished();
      case I_ZOMBIE -> izombie.isFinished();
      case BEGHOULED -> beghouled.isFinished();
      default -> true;
    };
  }

  private boolean engineWon() {
    return switch (type) {
      case VASEBREAKER -> vasebreaker.isWon();
      case WALLNUT_BOWLING -> bowling.isWon();
      case I_ZOMBIE -> izombie.isWon();
      case BEGHOULED -> beghouled.isWon();
      default -> false;
    };
  }
}
