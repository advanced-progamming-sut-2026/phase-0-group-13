package view.gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Scaling;
import java.util.LinkedHashMap;
import java.util.Map;
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
import view.gdx.ui.UiSkinProvider;


/**
 * Graphical Adventure map: pick a chapter, then a level inside it.
 *
 * <p>Chapter and level locks come from the account (getUnlockedStages, Progress.isLevelAccessible),
 * so they match what GameMenuController enforces for the typed "enter chapter" command.
 */
public final class AdventureScreen extends MenuScreen {

  /** Thumbnail size, roughly the 16:9 of the world art it is cropped from. */
  private static final float THUMB_WIDTH = 156f;
  private static final float THUMB_HEIGHT = 88f;

  private Table content;
  private int openChapter;
  /** One atlas per chapter thumbnail, opened lazily and disposed with the screen. */
  private final Map<Integer, TextureAtlas> worldArt = new LinkedHashMap<>();

  public AdventureScreen(PvzGdxGame game) {
    this(game, 0);
  }

  /** Opens straight into a chapter's level grid, e.g. coming back from plant selection. */
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
    return new MainMenuScreen(game);
  }

  @Override
  protected String backgroundAtlasPath() {
    return "textures/environment/" + worldAtlas(openChapter == 0 ? 1 : openChapter);
  }

  @Override
  protected void buildContent(Table content) {
    this.content = content;
    refresh();
  }

  private void refresh() {
    content.clear();

    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      Table panel = panel();
      panel.add(new Label("error: no user logged in", skin, UiSkinProvider.LABEL_MEDIUM)).row();
      panel.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget()))).width(220f);
      content.add(panel);
      return;
    }

    Table list = panel();
    list.top();
    if (openChapter == 0) {
      buildChapterList(list, user);
    } else {
      buildLevelGrid(list, user);
    }

    ScrollPane scroll = new ScrollPane(list, skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    // maxHeight so a short level grid doesn't leave a huge empty panel.
    content.add(scroll).growX().maxHeight(520f).row();

    content.add(button("Back", UiSkinProvider.BUTTON_BROWN, this::back)).width(240f).padTop(8f);
  }

  private void back() {
    if (openChapter == 0) {
      go(backTarget());
      return;
    }
    openChapter = 0;
    refresh();
  }

  private void buildChapterList(Table list, User user) {
    for (int stage = 1; stage <= AdventureMap.MAX_STAGES; stage++) {
      boolean unlocked = user.getUnlockedStages().contains("stage_" + stage);
      list.add(chapterRow(user, stage, unlocked)).growX().padBottom(12f).row();
    }
  }

  /**
   * A slice of the chapter's own world art, as its thumbnail.
   *
   * <p>The repository already has each world's background -- it is what this very screen draws
   * behind itself -- so the four rows can show the place they lead to instead of only naming it.
   * Cropped rather than squashed: the art is a wide lawn and letterboxing it into a small box
   * would waste most of the thumbnail, so the cell clips and the image fills it.
   *
   * <p>A locked world is shown dimmed rather than hidden. Seeing where you are going is the point
   * of a world list, and a blank row would say less than a dark one.
   */
  private Table worldThumbnail(int stage, boolean unlocked) {
    TextureAtlas atlas = worldArt.get(stage);
    if (atlas == null) {
      String path = "textures/environment/" + worldAtlas(stage);
      if (!Gdx.files.internal(path).exists()) {
        return null;
      }
      atlas = new TextureAtlas(Gdx.files.internal(path));
      worldArt.put(stage, atlas);
    }
    TextureRegion region = atlas.findRegion("texture");
    if (region == null) {
      return null;
    }
    Image art = new Image(region);
    art.setScaling(Scaling.fill);
    if (!unlocked) {
      art.setColor(0.42f, 0.42f, 0.48f, 1f);
    }

    Table frame = new Table();
    frame.setClip(true);
    frame.add(art).grow();
    if (unlocked) {
      return frame;
    }
    // The game's own padlock over the dimmed art, so "locked" is a symbol and not only a shade.
    Stack stack = new Stack();
    stack.add(frame);
    Table badge = new Table();
    badge.add(new Image(skin.getDrawable(UiSkinProvider.LOCK_ICON))).size(30f, 39f);
    stack.add(badge);
    Table wrapper = new Table();
    wrapper.add(stack).grow();
    return wrapper;
  }

  /**
   * How far through a chapter the player is.
   *
   * <p>A locked chapter still shows the empty bar rather than nothing, so the four rows read as
   * one progression instead of one row plus three blanks.
   */
  private Table chapterProgress(int cleared, boolean unlocked) {
    Table holder = new Table();
    holder.left();
    ProgressBar bar = new ProgressBar(0f, AdventureMap.LEVELS_PER_STAGE, 1f, false, skin,
        cleared >= AdventureMap.LEVELS_PER_STAGE ? "xp_green" : "xp_yellow");
    bar.setValue(unlocked ? cleared : 0);
    bar.setAnimateDuration(0f);
    if (!unlocked) {
      bar.getColor().a = 0.45f;
    }
    holder.add(bar).width(320f).height(18f);
    return holder;
  }

  private Table chapterRow(User user, int stage, boolean unlocked) {
    Table row = new Table();
    row.left();

    int cleared = clearedLevels(user.getProgress(), stage);

    Table thumb = worldThumbnail(stage, unlocked);
    if (thumb != null) {
      row.add(thumb).size(THUMB_WIDTH, THUMB_HEIGHT).padRight(18f);
    }

    // Name over a progress bar rather than a name and a "0 / 4" a long way apart: the whole point
    // of a world list is how far through each world you are, and the row had a wide dead gap in
    // the middle doing nothing.
    Table title = new Table();
    title.left();
    title.add(new Label(stage + ".  " + chapterName(stage), skin, UiSkinProvider.LABEL_BIG))
        .left().row();
    title.add(chapterProgress(cleared, unlocked)).left().padTop(4f).row();
    row.add(title).left().expandX();

    String progress = cleared + " / " + AdventureMap.LEVELS_PER_STAGE + " levels";
    row.add(new Label(unlocked ? progress : "LOCKED", skin, UiSkinProvider.LABEL_MEDIUM))
        .right()
        .padRight(20f);

    final int target = stage;
    if (unlocked) {
      row.add(button("Enter", UiSkinProvider.BUTTON_GREEN, () -> {
        // Same as the terminal's "enter chapter": a fresh seed bank for whichever level gets
        // picked next, so a deck chosen for a different chapter doesn't leak in.
        user.clearDeck();
        MatchSetup.getInstance().setTargetChapter(String.valueOf(target));
        openChapter = target;
        refresh();
      })).width(180f);
    } else {
      TextButton locked = new TextButton("Locked", skin, UiSkinProvider.BUTTON_BROWN);
      locked.setDisabled(true);
      row.add(locked).width(180f);
    }
    return row;
  }

  private void buildLevelGrid(Table list, User user) {
    Progress progress = user.getProgress();
    list.add(new Label("Choose a level", skin, UiSkinProvider.LABEL_MEDIUM))
        .left()
        .padBottom(12f)
        .row();

    Table grid = new Table();
    grid.defaults().pad(6f).width(150f);
    for (int level = 1; level <= AdventureMap.LEVELS_PER_STAGE; level++) {
      grid.add(levelButton(progress, level));
      if (level % 5 == 0) {
        grid.row();
      }
    }
    list.add(grid).row();
  }

  private TextButton levelButton(Progress progress, int level) {
    boolean accessible = progress.isLevelAccessible(openChapter, level);
    boolean cleared = level <= clearedLevels(progress, openChapter);

    if (!accessible) {
      TextButton locked = new TextButton("Level " + level + "\nLocked", skin,
              UiSkinProvider.BUTTON_BROWN);
      locked.setDisabled(true);
      return locked;
    }
    String label = cleared ? "Level " + level + "\ncleared" : "Level " + level;
    final int target = level;
    return button(label, UiSkinProvider.BUTTON_GREEN, () -> startLevel(target));
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
  private int clearedLevels(Progress progress, int stage) {
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
    for (TextureAtlas atlas : worldArt.values()) {
      atlas.dispose();
    }
    worldArt.clear();
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
