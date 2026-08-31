package view.gdx.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import data.persistence.UserManager;
import model.account.AdventureMap;
import model.account.Progress;
import model.account.User;
import model.core.GameManager;
import model.core.GameSession;
import model.core.MatchLauncher;
import model.core.MatchSetup;
import model.environment.AncientEgyptSeason;
import model.environment.BigWaveBeachSeason;
import model.environment.DarkAgesSeason;
import model.environment.FrostbiteCavesSeason;
import model.environment.Season;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.LevelMap;
import view.gdx.ui.MapArt;
import view.gdx.ui.UiSkinProvider;
import view.gdx.ui.WorldCarousel;


/**
 * The Adventure map: a chapter chooser, and inside a chapter the route of its levels.
 *
 * <p>Chapter and level locks come from the account (getUnlockedStages, Progress.isLevelAccessible),
 * so they match what GameMenuController enforces for the typed "enter chapter" command.
 */
public final class AdventureScreen extends MenuScreen {

  private final int openChapter;
  private static final float ARROW_WIDTH = 64f;
  private static final float ARROW_HEIGHT = 92f;

  private final MapArt art = new MapArt();

  private int chosenLevel;
  private TextButton playButton;
  private Runnable onChapterChanged;
  private Runnable onLevelChanged;

  public AdventureScreen(PvzGdxGame game) {
    this(game, 0);
  }

  /** Opens straight into a chapter's level map, e.g. coming back from plant selection. */
  public AdventureScreen(PvzGdxGame game, int openChapter) {
    super(game);
    this.openChapter = openChapter;
  }

  @Override
  protected String title() {
    return openChapter == 0 ? "Adventure" : chapterName(openChapter);
  }

  @Override
  protected Screen backTarget() {
    return openChapter == 0 ? new MainMenuScreen(game) : new AdventureScreen(game);
  }

  @Override
  protected String backgroundAtlasPath() {
    return "textures/environment/" + worldAtlas(openChapter == 0 ? 1 : openChapter);
  }

  @Override
  protected String backgroundImagePath() {
    return "textures/ui/menubackground.png";
  }

  /** The adventure backdrop is meant to be seen as it is, so nothing washes over it. */
  @Override
  protected boolean scrimBackground() {
    return false;
  }

  @Override
  protected void buildContent(Table content) {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      Table panel = panel();
      panel.add(new Label("error: no user logged in", skin, UiSkinProvider.LABEL_MEDIUM)).row();
      panel.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(new MainMenuScreen(game))))
          .width(220f);
      content.add(panel);
      return;
    }
    if (openChapter == 0) {
      buildChapterChooser(content, user);
    } else {
      buildLevelMap(content, user);
    }
  }

  // ---- chapter chooser -------------------------------------------------------------------

  private void buildChapterChooser(Table content, User user) {
    Table footer = new Table();

    WorldCarousel carousel = new WorldCarousel(skin, art, AdventureMap.MAX_STAGES,
        startingChapter(user), new WorldCarousel.State() {
          @Override
          public boolean locked(int stage) {
            return !isUnlocked(user, stage);
          }

          @Override
          public String summary(int stage) {
            if (!isUnlocked(user, stage)) {
              return "LOCKED";
            }
            return clearedLevels(user.getProgress(), stage)
                + " / " + AdventureMap.LEVELS_PER_STAGE + " levels";
          }
        }, new WorldCarousel.Listener() {
          @Override
          public void onSelected(int stage) {
            if (onChapterChanged != null) {
              onChapterChanged.run();
            }
          }

          @Override
          public void onEntered(int stage) {
            openChapter(user, stage);
          }
        });

    TextButton prev = button("<", UiSkinProvider.BUTTON_BROWN, () -> {});
    TextButton next = button(">", UiSkinProvider.BUTTON_BROWN, () -> {});
    Runnable syncArrows = () -> {
      prev.setDisabled(!carousel.canStep(-1));
      next.setDisabled(!carousel.canStep(1));
    };
    onChapterChanged = syncArrows;
    prev.addListener(stepper(carousel, -1, syncArrows));
    next.addListener(stepper(carousel, 1, syncArrows));
    arrowKeys(direction -> {
      carousel.step(direction);
      syncArrows.run();
    });
    syncArrows.run();

    content.add(sideBySide(prev, carousel, next)).grow().row();
    footer.defaults().pad(6f).width(230f);
    footer.add(button("Travel Log", UiSkinProvider.BUTTON_GREEN,
        () -> go(new QuestScreen(game))));
    footer.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(new MainMenuScreen(game))));
    content.add(footer).padTop(4f);
  }

  private void openChapter(User user, int stage) {
    if (!isUnlocked(user, stage)) {
      toast("Chapter " + stage + " is locked. Clear the previous chapter first.");
      return;
    }
    // Same as the terminal's "enter chapter": a fresh seed bank for whichever level gets picked,
    // so a deck chosen for a different chapter doesn't leak in.
    user.clearDeck();
    MatchSetup.getInstance().setTargetChapter(String.valueOf(stage));
    go(new AdventureScreen(game, stage));
  }

  /**
   * The wide selection area with an arrow at each edge.
   *
   * <p>The arrows go in a layer above it so they stay clickable and visible over the artwork, and
   * the area itself is inset by their width so nothing is ever hidden underneath one.
   */
  private Stack sideBySide(TextButton prev, com.badlogic.gdx.scenes.scene2d.Actor middle,
      TextButton next) {
    Table inset = new Table();
    inset.add(middle).grow().pad(0f, ARROW_WIDTH + 10f, 0f, ARROW_WIDTH + 10f);

    Table arrows = new Table();
    arrows.add(prev).width(ARROW_WIDTH).height(ARROW_HEIGHT).expandX().left();
    arrows.add(next).width(ARROW_WIDTH).height(ARROW_HEIGHT).expandX().right();

    Stack stack = new Stack();
    stack.add(inset);
    stack.add(arrows);
    return stack;
  }

  private ClickListener stepper(Object target, int direction, Runnable after) {
    return new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        if (target instanceof WorldCarousel carousel) {
          carousel.step(direction);
        } else if (target instanceof LevelMap map) {
          map.step(direction);
        }
        after.run();
      }
    };
  }

  /** Left and right arrow keys drive the same selection the arrow buttons do. */
  private void arrowKeys(java.util.function.IntConsumer step) {
    stage.addListener(new InputListener() {
      @Override
      public boolean keyDown(InputEvent event, int keycode) {
        if (keycode == Input.Keys.LEFT) {
          step.accept(-1);
          return true;
        }
        if (keycode == Input.Keys.RIGHT) {
          step.accept(1);
          return true;
        }
        return false;
      }
    });
  }

  /** Opens on the chapter the player is actually playing. */
  private static int startingChapter(User user) {
    int stage = user.getProgress().getCurrentStage();
    return stage < 1 || stage > AdventureMap.MAX_STAGES ? 1 : stage;
  }

  private static boolean isUnlocked(User user, int stage) {
    return user.getUnlockedStages().contains("stage_" + stage);
  }

  // ---- level map -------------------------------------------------------------------------

  private void buildLevelMap(Table content, User user) {
    Progress progress = user.getProgress();
    chosenLevel = firstPlayable(progress);

    LevelMap map = new LevelMap(skin, art, openChapter, AdventureMap.LEVELS_PER_STAGE,
        new LevelMap.Source() {
          @Override
          public LevelMap.NodeState stateOf(int level) {
            if (!progress.isLevelAccessible(openChapter, level)) {
              return LevelMap.NodeState.LOCKED;
            }
            if (level <= clearedLevels(progress, openChapter)) {
              return LevelMap.NodeState.COMPLETED;
            }
            return level == AdventureMap.LEVELS_PER_STAGE
                ? LevelMap.NodeState.BOSS : LevelMap.NodeState.AVAILABLE;
          }

          @Override
          public int currentLevel() {
            return chosenLevel;
          }
        }, level -> {
          if (level == chosenLevel) {
            startLevel(level);
            return;
          }
          if (onLevelChanged != null) {
            onLevelChanged.run();
          }
        });

    playButton = button("Play Level " + chosenLevel, UiSkinProvider.BUTTON_GREEN,
        () -> startLevel(chosenLevel));

    TextButton prev = button("<", UiSkinProvider.BUTTON_BROWN, () -> {});
    TextButton next = button(">", UiSkinProvider.BUTTON_BROWN, () -> {});
    Runnable syncArrows = () -> {
      chosenLevel = map.selectedLevel();
      prev.setDisabled(!map.canStep(-1));
      next.setDisabled(!map.canStep(1));
      boolean playable = progress.isLevelAccessible(openChapter, chosenLevel);
      playButton.setText(playable ? "Play Level " + chosenLevel : "Level " + chosenLevel + " locked");
      playButton.setDisabled(!playable);
    };
    onLevelChanged = syncArrows;
    prev.addListener(stepper(map, -1, syncArrows));
    next.addListener(stepper(map, 1, syncArrows));
    arrowKeys(direction -> {
      map.step(direction);
      syncArrows.run();
    });
    map.select(chosenLevel);
    syncArrows.run();

    content.add(sideBySide(prev, map, next)).grow().row();
    Table footer = new Table();
    footer.defaults().pad(6f).width(230f);
    footer.add(playButton);
    footer.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(new AdventureScreen(game))));
    content.add(footer).padTop(4f);
  }

  /** The level the map opens on: the first one not yet cleared, else the last. */
  private int firstPlayable(Progress progress) {
    int cleared = clearedLevels(progress, openChapter);
    for (int level = cleared + 1; level <= AdventureMap.LEVELS_PER_STAGE; level++) {
      if (progress.isLevelAccessible(openChapter, level)) {
        return level;
      }
    }
    return Math.max(1, Math.min(cleared, AdventureMap.LEVELS_PER_STAGE));
  }

  // The chapter has to be in MatchSetup before the selection screen opens: selectionRule() reads
  // it to work out which plants this stage locks out. The Conveyor Belt level hands out plants
  // itself, so it skips selection and goes straight to the lawn.
  private void startLevel(int level) {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      toast("error: no user logged in");
      return;
    }
    if (!user.getProgress().isLevelAccessible(openChapter, level)) {
      toast("Level " + level + " is locked.");
      return;
    }
    MatchSetup.getInstance().setTargetChapter(String.valueOf(openChapter));
    MatchSetup.getInstance().setTargetLevel(level);
    MatchSetup.getInstance().setDifficultyLevel(user.getDifficultyLevel());

    if (!MatchLauncher.skipsPlantSelection(openChapter, level)) {
      go(new PlantSelectionScreen(game, openChapter));
      return;
    }

    // No selection screen ran, and entering the chapter cleared the deck, so the belt draws from
    // everything the player owns rather than from an empty seed bank.
    MatchSetup.getInstance().setSelectedPlants(user.getUnlockedPlants());
    MatchSetup.getInstance().setBoostedPlants(user.getBoostedPlants());
    MatchLauncher.launch();
    GameManager started = GameSession.getActiveGame();
    if (started == null) {
      toast("could not start the level");
      return;
    }
    go(new GameplayScreen(game, started));
  }

  /** Levels of this chapter the player has already cleared. */
  private static int clearedLevels(Progress progress, int stage) {
    // The cursor stops on 4-4 rather than running off the map, so once the adventure is finished
    // "levels cleared" has to come from the flag or the last chapter would read 3 / 4 forever.
    if (progress.isAdventureCompleted()) {
      return AdventureMap.LEVELS_PER_STAGE;
    }
    if (stage < progress.getCurrentStage()) {
      return AdventureMap.LEVELS_PER_STAGE;
    }
    if (stage > progress.getCurrentStage()) {
      return 0;
    }
    return Math.max(0, progress.getCurrentLevel() - 1);
  }

  /** Same stage-to-season mapping MatchLauncher uses. */
  private static String chapterName(int stage) {
    Season season = switch (stage) {
      case 1 -> new AncientEgyptSeason();
      case 2 -> new FrostbiteCavesSeason();
      case 3 -> new BigWaveBeachSeason();
      default -> new DarkAgesSeason();
    };
    return season.getName();
  }

  @Override
  public void dispose() {
    art.dispose();
    super.dispose();
  }

  private static String worldAtlas(int stage) {
    return switch (stage) {
      case 1 -> "ancientegyptseason.atlas";
      case 2 -> "frostbitecavesseason.atlas";
      case 3 -> "bigwavebeachseason.atlas";
      default -> "darkagesseason.atlas";
    };
  }
}
