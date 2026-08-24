package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import data.persistence.UserManager;
import java.util.List;
import model.account.Progress;
import model.account.User;
import model.core.GameManager;
import model.core.GameSession;
import model.core.MatchSetup;
import model.core.MiniGameLauncher;
import model.enums.MiniGameType;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.UiSkinProvider;


/**
 * The mini-game half of the Travel Log, which used to be terminal-only.
 *
 * <p>The unlock and level rules are Progress's, the same ones QuestMenuController checks for
 * "play minigame". The four arcade modes open {@link ArcadeMiniGameScreen}; Zombotany is a normal
 * stage, so {@link MiniGameLauncher} builds it and it plays on GameplayScreen.
 */
public final class MiniGamesScreen extends MenuScreen {

  private static final List<String> GAMES =
      List.of("vasebreaker", "wallnut_bowling", "i_zombie", "beghouled", "zombotany");

  public MiniGamesScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  protected String title() {
    return "Mini-Games";
  }

  @Override
  protected Screen backTarget() {
    return new QuestScreen(game);
  }

  @Override
  protected void buildContent(Table content) {
    User user = UserManager.getInstance().getCurrentUser();
    Table panel = panel();
    panel.top();

    if (user == null) {
      panel.add(new Label("error: no user logged in", skin, UiSkinProvider.LABEL_MEDIUM)).row();
      panel.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget()))).width(220f);
      content.add(panel);
      return;
    }

    Progress progress = user.getProgress();
    if (!progress.isMiniGamesUnlocked()) {
      panel.add(new Label("Mini-Games mode is still locked. Keep clearing stages to unlock it!",
              skin, UiSkinProvider.LABEL_MEDIUM)).row();
    } else {
      for (String name : GAMES) {
        panel.add(gameRow(user, progress, name)).growX().padBottom(10f).row();
      }
    }

    content.add(panel).growX().row();
    content.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())))
        .width(220f)
        .padTop(8f);
  }

  private Table gameRow(User user, Progress progress, String name) {
    Table row = new Table();
    row.left();
    boolean unlocked = progress.isMiniGameUnlocked(name);
    int cleared = progress.getClearedMiniGameLevel(name);

    row.add(new Label(name.replace('_', ' '), skin, UiSkinProvider.LABEL_BIG)).left().expandX();
    row.add(new Label(unlocked ? cleared + " / " + Progress.MINI_GAME_LEVELS + " cleared"
            : "LOCKED", skin, UiSkinProvider.LABEL_MEDIUM)).right().padRight(18f);

    Table levels = new Table();
    levels.defaults().pad(4f).width(90f).height(54f);
    for (int level = 1; level <= Progress.MINI_GAME_LEVELS; level++) {
      levels.add(levelButton(user, name, level, unlocked));
    }
    row.add(levels).right();
    return row;
  }

  private TextButton levelButton(User user, String name, int level, boolean unlocked) {
    if (!unlocked) {
      TextButton locked = new TextButton(String.valueOf(level), skin, UiSkinProvider.BUTTON_BROWN);
      locked.setDisabled(true);
      return locked;
    }
    return button(String.valueOf(level), UiSkinProvider.BUTTON_GREEN, () -> play(user, name, level));
  }

  private void play(User user, String name, int level) {
    MiniGameType type = typeOf(name);
    if (type == MiniGameType.NONE) {
      toast("error: unknown mini-game: " + name);
      return;
    }
    MatchSetup.getInstance().setMiniGame(type, level);
    if (type != MiniGameType.ZOMBOTANY) {
      go(new ArcadeMiniGameScreen(game, type, level));
      return;
    }

    // Zombotany is an ordinary stage. There is no deck builder on this route, so the seed bank is
    // everything the player owns, same as the Conveyor Belt level does on the Adventure map.
    MatchSetup.getInstance().setSelectedPlants(user.getUnlockedPlants());
    MatchSetup.getInstance().setBoostedPlants(user.getBoostedPlants());
    MatchSetup.getInstance().setDifficultyLevel(user.getDifficultyLevel());
    MiniGameLauncher.launch();
    GameManager started = GameSession.getActiveGame();
    if (started == null) {
      toast("could not start the mini-game");
      return;
    }
    go(new GameplayScreen(game, started));
  }

  private static MiniGameType typeOf(String name) {
    return switch (name) {
      case "vasebreaker" -> MiniGameType.VASEBREAKER;
      case "wallnut_bowling" -> MiniGameType.WALLNUT_BOWLING;
      case "i_zombie" -> MiniGameType.I_ZOMBIE;
      case "beghouled" -> MiniGameType.BEGHOULED;
      case "zombotany" -> MiniGameType.ZOMBOTANY;
      default -> MiniGameType.NONE;
    };
  }
}
