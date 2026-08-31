package view.gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import data.GameDataManager;
import data.persistence.UserManager;
import java.util.ArrayList;
import java.util.List;
import model.account.User;
import model.game.MatchResult;
import model.game.minigame.BossStageRule;
import model.game.minigame.ConveyorRule;
import model.game.minigame.DeadLineRule;
import model.game.minigame.LoveYourPlantsRule;
import model.game.minigame.PlantWhatYouGetRule;
import model.game.minigame.SaveOurSeedsRule;
import model.game.minigame.SpecialStageRule;
import model.game.minigame.TimedWarRule;
import model.game.plant.Plant;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.zombie.Zombie;
import model.core.BonusGameLauncher;
import model.core.GameManager;
import model.core.GameSession;
import model.core.MatchCompletion;
import model.core.MatchLauncher;
import model.core.MatchSetup;
import model.core.MiniGameLauncher;
import model.enums.MiniGameType;
import view.gdx.audio.GameAudio;
import view.gdx.core.FixedStepClock;
import view.gdx.core.GameSettings;
import view.gdx.core.GdxConfig;
import view.gdx.core.PvzGdxGame;
import view.gdx.input.GameplayInputHandler;
import view.gdx.input.GdxGameActions;
import view.gdx.render.CursorRenderer;
import view.gdx.render.EntityRenderer;
import view.gdx.render.LawnGeometry;
import view.gdx.render.LawnRenderer;
import view.gdx.render.StageRuleRenderer;
import view.gdx.ui.CountUpLabel;
import view.gdx.ui.Dialogue;
import view.gdx.ui.HudArt;
import view.gdx.ui.HudStage;
import view.gdx.ui.Popup;
import view.gdx.ui.UiSkinProvider;

/**
 * The in-match screen.
 *
 * <p>The match itself is the same GameManager the terminal build runs, stepped through
 * GamePlayController so planting rules, waves, win/loss and the end-of-match rewards are not
 * duplicated here.
 *
 * <p>Two clocks on purpose: FixedStepClock runs the simulation at the model's tick rate, and the
 * frame delta is only for drawing.
 *
 * <p>Pausing stops both: no tick, and the renderers get a delta of zero so animations freeze
 * too. Only the HUD keeps acting, since the pause menu lives in it.
 */
public final class GameplayScreen extends BaseScreen {

  private final GameManager match;
  private final FixedStepClock clock = new FixedStepClock(GdxConfig.SECONDS_PER_TICK);
  private final LawnGeometry geometry;

  private LawnRenderer lawnRenderer;
  private EntityRenderer entityRenderer;
  private StageRuleRenderer stageRuleRenderer;
  private CursorRenderer cursorRenderer;
  private HudStage hud;
  /** Only for the result panel's trophy and brain; the HUD keeps its own copy. */
  private final HudArt hudArt = new HudArt();
  private GameplayInputHandler input;
  private GdxGameActions actions;
  private boolean ended;
  private boolean paused;
  /** The run's MyoPoints, read at the whistle before the account banks (and clears) them. */
  private int bonusScore;
  /** How many zombies were chewing last frame; see listenForBites. */
  private int zombiesEating;
  /** True while the level-start popups are up, so nobody loses the lawn while reading them. */
  private boolean intro;
  private int lastWave;

  public GameplayScreen(PvzGdxGame game, GameManager match) {
    super(game);
    this.match = match;
    this.geometry = new LawnGeometry(boardRows(match), boardColumns(match));
  }

  @Override
  public com.badlogic.gdx.scenes.scene2d.Stage uiStage() {
    return hud == null ? null : hud.getStage();
  }

  @Override
  public void show() {
    GameAudio.getInstance().playMusic(GameAudio.Track.BATTLE);
    lawnRenderer = new LawnRenderer(geometry);
    entityRenderer = new EntityRenderer(geometry);
    stageRuleRenderer = new StageRuleRenderer(geometry);

    hud = new HudStage();
    hud.build(game.getUiSkin(), this::leave, match);
    // A boss stage shows Zomboss's health where every other stage shows the wave meter.
    if (bossRule() != null) {
      hud.buildBossBar(Dialogue.zombossTitle(seasonName()));
    } else {
      hud.buildWaveBar(match == null ? 1 : match.getTotalWaves());
    }

    // refusals go to the HUD toast, not a console the player never sees
    actions = new GdxGameActions(match, this::leave, hud::toast);
    input = new GameplayInputHandler(context(), geometry, actions, this::togglePause);
    cursorRenderer = new CursorRenderer(geometry, input);

    buildBar();
    if (match != null && !match.isZombieWavesStarted()) {
      hud.buildStartWavesButton(this::startWaves);
    }
    clock.reset();

    Gdx.input.setInputProcessor(new InputMultiplexer(hud.getStage(), input));
    layout();
    showBriefing();
  }

  /** Boss stages run on the belt too, so this asks the rule for one rather than checking its type. */
  private ConveyorRule belt() {
    return match == null || match.getSpecialStageRule() == null
        ? null : match.getSpecialStageRule().belt();
  }

  private BossStageRule bossRule() {
    return match != null && match.getSpecialStageRule() instanceof BossStageRule boss ? boss : null;
  }

  /** The belt hands out its own plants, so on a conveyor stage it stands in for the seed bank. */
  private void buildBar() {
    ConveyorRule belt = belt();
    if (belt != null) {
      hud.buildConveyorBar(belt, input::setSelectedPlantType, input::toggleShovel,
          input::togglePlantFood, this::togglePause);
      return;
    }
    List<PlantTemplate> deck = deckTemplates();
    hud.buildSeedBar(game.getUiSkin().get(), deck, input::setSelectedPlantType,
        input::toggleShovel, input::togglePlantFood, this::togglePause);
    input.setSeedOrder(deckNames(deck));
  }

  /** Ends the free build phase of a "whatever comes" stage. */
  private void startWaves() {
    if (match == null) {
      return;
    }
    match.startZombieWaves();
    hud.setStartWavesVisible(false);
    hud.alert("Here they come!");
  }

  private static List<PlantTemplate> deckTemplates() {
    List<PlantTemplate> templates = new ArrayList<>();
    if (GameDataManager.plantRepository == null) {
      return templates;
    }
    for (String name : MatchSetup.getInstance().getSelectedPlants()) {
      PlantTemplate template = GameDataManager.plantRepository.find(name);
      if (template != null) {
        templates.add(template);
      }
    }
    return templates;
  }

  /** The same order the seed bar draws, so key 1 arms the leftmost card. */
  private static List<String> deckNames(List<PlantTemplate> templates) {
    List<String> names = new ArrayList<>();
    for (PlantTemplate template : templates) {
      names.add(template.name);
    }
    return names;
  }

  private static int plantLevel(String plant) {
    User user = UserManager.getInstance().getCurrentUser();
    return user == null ? 1 : Math.max(1, user.getPlantLevel(plant));
  }

  /** True for the daily score run, which belongs to the main menu rather than the adventure map. */
  private boolean isBonus() {
    return match != null && match.isBonusMatch();
  }

  /** Zombotany is an ordinary stage, so it plays here but belongs to the mini-game menu. */
  private boolean isMiniGame() {
    return MatchSetup.getInstance().getCurrentMiniGame() != MiniGameType.NONE;
  }

  private void leave() {
    GameSession.end();
    if (isBonus()) {
      game.switchScreen(new MainMenuScreen(game));
    } else if (isMiniGame()) {
      game.switchScreen(new MiniGamesScreen(game));
    } else {
      game.switchScreen(new AdventureScreen(game));
    }
  }

  /** Save & Exit: keep whatever the match already awarded, then back out. */
  private void saveAndLeave() {
    runAsync(
        () -> {
          UserManager.getInstance().updateCurrentUserGameState();
          return null;
        },
        ignored -> leave(),
        e -> {
          // worth saying, but must not trap the player in the match
          hud.toast(e.getMessage() == null ? "could not save your progress" : e.getMessage());
          leave();
        });
  }

  /** Rebuilds the level from the same MatchSetup. */
  private void restart() {
    boolean bonus = isBonus();
    boolean miniGame = isMiniGame();
    GameSession.end();
    if (bonus) {
      BonusGameLauncher.launch();
    } else if (miniGame) {
      MiniGameLauncher.launch();
    } else {
      MatchLauncher.launch();
    }
    GameManager restarted = GameSession.getActiveGame();
    if (restarted == null) {
      leave();
      return;
    }
    game.switchScreen(new GameplayScreen(game, restarted));
  }

  private void togglePause() {
    if (ended) {
      return;
    }
    paused = !paused;
    input.setPaused(paused);
    if (paused) {
      hud.showPauseMenu(this::togglePause, this::restart, this::saveAndLeave);
    } else {
      // do not let the match catch up on time spent in the menu
      clock.reset();
    }
  }

  /** Shown once when the level starts: what to expect this level, per the Phase 2 spec. */
  private void showBriefing() {
    if (match == null || game.getUiSkin().get() == null) {
      return;
    }
    intro = true;
    Label text = new Label(briefingText(), game.getUiSkin().get(), UiSkinProvider.LABEL_MEDIUM);
    text.setWrap(true);
    Table body = new Table();
    body.add(text).width(420f);
    Popup.show(hud.getStage(), game.getUiSkin().get(), "Level start", body,
        new Popup.Choice("Let's go", UiSkinProvider.BUTTON_GREEN, this::showStageDialogue));
  }

  /** Penny sets the scene, then the red banner says what happens next. */
  private void showStageDialogue() {
    Dialogue.show(hud.getStage(), game.getUiSkin().get(), Dialogue.PENNY,
        Dialogue.stageStart(seasonName(), playerName()), this::beginPlaying);
  }

  private static String playerName() {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      return null;
    }
    return user.getNickname() == null || user.getNickname().isBlank()
        ? user.getUsername()
        : user.getNickname();
  }

  private void beginPlaying() {
    intro = false;
    clock.reset();
    hud.alert(match.isZombieWavesStarted()
        ? "The first wave is on its way!"
        : "Plant freely, then start the waves when you are ready.");
  }

  private String seasonName() {
    return match == null || match.getSeason() == null ? "" : match.getSeason().getName();
  }

  /** The boss's health, or nothing at all until it rolls on. */
  private void updateBossBar() {
    BossStageRule boss = bossRule();
    if (boss == null) {
      return;
    }
    Zombie zomboss = boss.getBoss();
    if (zomboss == null) {
      return;
    }
    hud.updateBoss(boss.getBossHealth(), zomboss.getCurrentHealth(), boss.isBossStunned());
  }

  private String briefingText() {
    StringBuilder text = new StringBuilder();
    if (isBonus()) {
      // Same seed for everyone today, which is the whole point of scoring it against other players.
      text.append("Today's Bonus Game: every player faces the same ")
          .append(match.getTotalWaves())
          .append(" waves.\n")
          .append("Score as many points as you can before they reach your house.\n")
          .append("Your best run goes to the server and shows as My Point on the leaderboard.");
      return text.toString();
    }
    text.append(match.getTotalWaves()).append(" waves of zombies incoming.\n");
    if (match.getBoard() != null && match.getBoard().getGameState().isSkySunDisabled()) {
      text.append("No sun will fall from the sky -- grow your own.\n");
    }
    SpecialStageRule rule = match.getSpecialStageRule();
    if (rule instanceof BossStageRule) {
      text.append("Boss stage: the belt feeds you plants, there is no seed bank.\n")
          .append(Dialogue.bossWarning(seasonName())).append('\n')
          .append("Break all three of his health segments to take the chapter.");
      return text.toString();
    }
    if (rule != null) {
      text.append(rule.getClass().getSimpleName()).append(" is active this level.\n");
    }
    text.append("Defend your lawn -- don't let the zombies reach the house!");
    return text.toString();
  }

  private void layout() {
    float[] box = LawnRenderer.lawnBounds(match);
    geometry.setBounds(box[0], box[1], box[2], box[3]);
  }

  @Override
  public void render(float delta) {
    if (!paused && !intro && match != null && match.isRunning()) {
      // speed is a view setting: more ticks per frame, same tick rate in the model
      clock.update(delta * GameSettings.getGameSpeed(), actions::advanceOneTick);
    }
    // renderers keep their own animation clocks, so freezing means giving them no time
    float worldDelta = paused || intro ? 0f : delta;

    input.updateHover(Gdx.input.getX(), Gdx.input.getY());
    if (!paused && !intro) {
      // Suns are collected by passing the pointer over them, so this is a frame job, not a click.
      input.collectSunUnderPointer();
    }

    context().applyCamera();
    lawnRenderer.render(context(), match, worldDelta);
    // under the entities: these mark cells, they shouldn't cover what stands on them
    stageRuleRenderer.render(context(), match, worldDelta);
    entityRenderer.render(context(), match, worldDelta);
    if (!paused) {
      cursorRenderer.render(context(), match, worldDelta);
    }

    watchWaves();
    showPickups();
    listenForBites();
    updateStatus();
    hud.updateWave(match == null ? 0 : match.getCurrentWaveIndex());
    updateBossBar();
    hud.updateConveyor();
    hud.updateSeeds(match, input.getSelectedPlantType(), GameplayScreen::plantLevel);
    hud.updateTools(input.getTool() == GameplayInputHandler.Tool.SHOVEL,
        input.getTool() == GameplayInputHandler.Tool.PLANT_FOOD,
        match == null ? 0 : match.getPlantFoodCount());
    if (match != null && !match.isRunning()) {
      showResult();
    }

    hud.act(delta);
    hud.draw();
  }

  private void updateStatus() {
    if (match == null) {
      hud.setStatus("no match running");
      return;
    }
    // Ahead of the paused check: the counter is frozen either way, but it should read the real
    // total rather than whatever it happened to show when the menu opened.
    hud.setSun(match.getSunAmount());
    hud.setObjective(objectiveText());
    if (paused) {
      hud.setStatus("paused");
      return;
    }
    // the wave count moved to the wave bar
    hud.setStatus("plant food " + match.getPlantFoodCount() + nightNote() + hint());
  }

  /** Pickups are collected for the player, so they have to be told. */
  private void showPickups() {
    if (match == null || match.getBoard() == null) {
      return;
    }
    for (String pickup : match.getBoard().drainPendingNotices()) {
      hud.toast(pickup);
    }
  }

  /**
   * Bites the player can hear.
   *
   * <p>Observed rather than pushed: the model has no audio channel and should not grow one, so
   * this counts the zombies that are chewing this frame and only makes a noise when that count
   * goes up -- a zombie that keeps eating is one bite, not one per frame. GameAudio's repeat guard
   * then collapses a whole wave arriving at once into a single sound.
   */
  private void listenForBites() {
    if (match == null || match.getBoard() == null || paused || intro) {
      return;
    }
    int eating = 0;
    for (Zombie zombie : match.getBoard().getZombies()) {
      if (zombie != null && !zombie.isDead() && zombie.isEating()) {
        eating++;
      }
    }
    if (eating > zombiesEating) {
      GameAudio.getInstance().play(GameAudio.Sfx.CHOMP);
    }
    zombiesEating = eating;
  }

  /** Red banner every time the match rolls into a new wave. */
  private void watchWaves() {
    if (match == null || paused || intro || !match.isRunning()) {
      return;
    }
    int wave = match.getCurrentWaveIndex();
    if (wave == lastWave || wave >= match.getTotalWaves()) {
      return;
    }
    lastWave = wave;
    hud.alert("Wave " + (wave + 1) + " of " + match.getTotalWaves() + waveWarning());
  }

  /** The two seasons that do something extra at the start of every wave say so. */
  private String waveWarning() {
    String season = seasonName().toLowerCase();
    if (season.contains("dark")) {
      return "   -   graves are rising, necromancy incoming!";
    }
    if (season.contains("beach") || season.contains("wave")) {
      return "   -   the tide is turning, zombies from the back coast!";
    }
    return "";
  }

  /** What this stage wants, for the rules the lawn doesn't explain on its own. */
  private String objectiveText() {
    if (isBonus()) {
      // The score is the objective here, so it goes where the objective line already is.
      return "MyoPoints " + match.getScoreManager().getCurrentMatchScore()
          + "   -   survive for as long as you can";
    }
    SpecialStageRule rule = match.getSpecialStageRule();
    if (rule instanceof TimedWarRule timed) {
      int survived = timed.getTimeLimitTicks() - timed.remainingTicks();
      return String.format("hold out %.1fs more   -   %d zombies left   -   %d%% survived",
          timed.remainingTicks() / 10.0, countZombies(),
          Math.round(100f * survived / Math.max(1, timed.getTimeLimitTicks())));
    }
    if (rule instanceof LoveYourPlantsRule love) {
      int left = love.getLossBudget() - match.getMatchContext().getPlantsLost();
      return "don't lose your plants   -   " + countPlants() + " on the lawn, "
          + Math.max(0, left) + " of " + love.getLossBudget() + " losses to spare";
    }
    if (rule instanceof DeadLineRule) {
      return "no zombie may cross the red line";
    }
    if (rule instanceof SaveOurSeedsRule) {
      return "keep every highlighted plant alive";
    }
    if (rule instanceof BossStageRule boss) {
      Zombie zomboss = boss.getBoss();
      if (zomboss == null) {
        return "the belt is your seed bank   -   Dr. Zomboss is on his way";
      }
      return "break all three of his health segments   -   "
          + boss.getBossHealth().segmentsLeft(zomboss.getCurrentHealth()) + " left"
          + (boss.isBossStunned() ? "   -   he is stunned, hit him now!" : "");
    }
    if (rule instanceof ConveyorRule) {
      return "plant whatever the belt gives you";
    }
    if (rule instanceof PlantWhatYouGetRule offer) {
      return (match.isZombieWavesStarted() ? "" : "free build   -   ")
          + "the lawn is handing you: " + offer.getCurrentOffer();
    }
    return match.isZombieWavesStarted()
        ? "" : "free build   -   plant what you like, then start the waves";
  }

  private int countZombies() {
    if (match.getBoard() == null) {
      return 0;
    }
    int alive = 0;
    for (Zombie zombie : match.getBoard().getZombies()) {
      if (!zombie.isDead()) {
        alive++;
      }
    }
    return alive;
  }

  private int countPlants() {
    if (match.getBoard() == null) {
      return 0;
    }
    int alive = 0;
    for (Plant plant : match.getBoard().getPlants()) {
      if (!plant.isDead()) {
        alive++;
      }
    }
    return alive;
  }

  /** What the next click will do. */
  private String hint() {
    switch (input.getTool()) {
      case SEED:
        return "   placing: " + input.getSelectedPlantType();
      case SHOVEL:
        return "   shovel armed - click a plant to dig it up";
      case PLANT_FOOD:
        return "   plant food armed - click a plant to feed it";
      default:
        return "   pick a seed, then click the lawn";
    }
  }

  /**
   * Dark Ages is at night and nothing falls out of the sky, so every sun has to come off a plant.
   * The background says "night" on its own but not "and that is why your sun is not going up".
   */
  private String nightNote() {
    return match.getBoard() != null && match.getBoard().getGameState().isSkySunDisabled()
        ? "   night: no sun falls from the sky"
        : "";
  }

  private void showResult() {
    if (ended) {
      return;
    }
    ended = true;
    // Same reason MatchCompletion reads it early: applying the run to the account zeroes it.
    bonusScore = match.getScoreManager().getCurrentMatchScore();
    MatchCompletion.apply(match);
    paused = false;
    if (game.getUiSkin().get() == null) {
      return;
    }
    boolean won = match.getMatchResult() != null && match.getMatchResult().isWon();
    // Crazy Dave gets the first word after a Zomboss goes down, then the result.
    if (won && match.getSpecialStageRule() instanceof BossStageRule) {
      Dialogue.show(hud.getStage(), game.getUiSkin().get(), Dialogue.CRAZY_DAVE,
          Dialogue.afterZomboss(seasonName()), () -> showOutcome(true));
      return;
    }
    showOutcome(won);
  }

  /**
   * The end-of-level panel.
   *
   * <p>It used to be one sentence, which told the player nothing about the run they had just
   * played and made a win look like an error dialog. The numbers here are all ones the match
   * already kept -- the score, the coins the result banked, how far through the waves it got --
   * so this reads them rather than computing anything, and the rewards roll up because the reward
   * is the point of the screen.
   */
  private void showOutcome(boolean won) {
    GameAudio.getInstance().play(won ? GameAudio.Sfx.WIN : GameAudio.Sfx.LOSE);
    Skin skin = game.getUiSkin().get();
    Table body = new Table();
    body.defaults().pad(2f);

    body.add(outcomeHeadline(skin, won)).padBottom(10f).row();

    body.add(summary(skin, won)).padBottom(4f).row();

    if (isBonus()) {
      // The run is scored either way here, so the number matters more than the verdict.
      body.add(new Label("Sent to the server; the leaderboard keeps your best.",
          skin, "secondary")).padTop(6f).row();
    }
    String leaveLabel = isBonus() ? "Back to menu" : "Back to map";
    if (won) {
      Popup.show(hud.getStage(), game.getUiSkin().get(), "You win", body,
          leaveLabel, this::leave, null, null);
      return;
    }
    Popup.show(hud.getStage(), game.getUiSkin().get(), "You lose", body,
        "Retry", this::restart, leaveLabel, this::leave);
  }

  /**
   * The verdict, with the game's own art beside it.
   *
   * <p>A win and a loss used to differ only in their wording. The gold cup is the same trophy the
   * leaderboard hands its top place, and the brain is the one the HUD already draws for the thing
   * the zombies are after -- so each result carries the object it is about, and the two screens
   * are told apart at a glance rather than by reading.
   */
  private Table outcomeHeadline(Skin skin, boolean won) {
    Table headline = new Table();
    TextureRegion mark = hudArt.find(won ? "cupgold" : "brain");
    if (mark != null) {
      Image image = new Image(mark);
      image.setScaling(Scaling.fit);
      headline.add(image).size(won ? 44f : 52f, 64f).padRight(14f);
    }
    headline.add(new Label(won ? "Level cleared!" : "The zombies ate your brains!",
        skin, UiSkinProvider.LABEL_BIG_OUTLINE));
    return headline;
  }

  /** The run, in the numbers the match already tracked. Rewards roll up; facts do not. */
  private Table summary(Skin skin, boolean won) {
    Table stats = new Table();
    stats.defaults().pad(3f);

    if (isBonus()) {
      stats.add(statRow(skin, "MyoPoints this run", new CountUpLabel(bonusScore, skin,
          UiSkinProvider.LABEL_BIG_OUTLINE), null)).row();
      return stats;
    }

    MatchResult result = match == null ? null : match.getMatchResult();
    int waves = match == null ? 0 : Math.min(match.getCurrentWaveIndex(), match.getTotalWaves());
    int totalWaves = match == null ? 0 : match.getTotalWaves();

    stats.add(statRow(skin, "Waves survived",
        new Label(waves + " / " + totalWaves, skin, UiSkinProvider.LABEL_MEDIUM), null)).row();

    if (result != null) {
      stats.add(statRow(skin, "Score",
          new CountUpLabel(result.getScore(), skin, UiSkinProvider.LABEL_MEDIUM), null)).row();
      // Only a win banks coins, so showing a zero on a loss would just be noise.
      if (won && result.getRewardCoins() > 0) {
        stats.add(statRow(skin, "Coins earned",
            new CountUpLabel(result.getRewardCoins(), skin, UiSkinProvider.LABEL_BIG_OUTLINE),
            UiSkinProvider.COIN_ICON)).row();
      }
    }
    return stats;
  }

  /** "label ....... [icon] value", so the numbers line up down the right. */
  private Table statRow(Skin skin, String label, Label value, String icon) {
    Table row = new Table();
    row.add(new Label(label, skin, "secondary")).right().width(190f).padRight(14f);
    if (icon != null) {
      row.add(new Image(skin.getDrawable(icon))).size(28f).padRight(8f);
    }
    row.add(value).left().width(120f);
    return row;
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);
    hud.resize(width, height);
    layout();
  }

  @Override
  public void hide() {
    Gdx.input.setInputProcessor(null);
  }

  @Override
  public void dispose() {
    if (lawnRenderer != null) {
      lawnRenderer.dispose();
    }
    if (entityRenderer != null) {
      entityRenderer.dispose();
    }
    if (stageRuleRenderer != null) {
      stageRuleRenderer.dispose();
    }
    if (cursorRenderer != null) {
      cursorRenderer.dispose();
    }
    if (hud != null) {
      hud.dispose();
    }
    hudArt.dispose();
  }

  private static int boardRows(GameManager match) {
    return match != null && match.getBoard() != null
        ? match.getBoard().getRows()
        : GdxConfig.LAWN_ROWS;
  }

  private static int boardColumns(GameManager match) {
    return match != null && match.getBoard() != null
        ? match.getBoard().getColumns()
        : GdxConfig.LAWN_COLUMNS;
  }
}
