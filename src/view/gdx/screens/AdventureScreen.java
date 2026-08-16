package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import data.persistence.UserManager;
import model.account.AdventureMap;
import model.account.Progress;
import model.account.User;
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

  private Table content;
  private int openChapter;

  public AdventureScreen(PvzGdxGame game) {
    super(game);
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

  private Table chapterRow(User user, int stage, boolean unlocked) {
    Table row = new Table();
    row.left();

    // Stages 4 and 5 are both Dark Ages, so the number is what tells the rows apart.
    row.add(new Label(stage + ".  " + chapterName(stage), skin, UiSkinProvider.LABEL_BIG))
        .left()
        .expandX();

    int cleared = clearedLevels(user.getProgress(), stage);
    String progress = cleared + " / " + AdventureMap.LEVELS_PER_STAGE + " levels";
    row.add(new Label(unlocked ? progress : "LOCKED", skin, UiSkinProvider.LABEL_MEDIUM))
        .right()
        .padRight(20f);

    final int target = stage;
    if (unlocked) {
      row.add(button("Enter", UiSkinProvider.BUTTON_GREEN, () -> {
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

  private void startLevel(int level) {
    MatchSetup.getInstance().setTargetChapter(String.valueOf(openChapter));
    toast("Entering " + chapterName(openChapter) + " - level " + level + "...");
    go(new GameplayScreen(game, null, null));
  }

  /** Levels of this chapter the player has already cleared. */
  private int clearedLevels(Progress progress, int stage) {
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

  private static String worldAtlas(int stage) {
    return switch (stage) {
      case 1 -> "ancientegyptseason.atlas";
      case 2 -> "frostbitecavesseason.atlas";
      case 3 -> "bigwavebeachseason.atlas";
      default -> "darkagesseason.atlas";
    };
  }
}
