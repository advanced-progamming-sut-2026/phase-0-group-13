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
import model.game.quest.Quest;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.zombie.Zombie;
import model.core.BonusGameLauncher;
import model.core.GameManager;
import model.core.GameSession;
import model.core.MatchCompletion;
import model.core.MatchLauncher;
import model.core.MatchSaveManager;
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

public final class GameplayScreen extends BaseScreen {

  /** How many open quests the level-start briefing lists before it gets too long to read. */
  private static final int MAX_BRIEFING_QUESTS = 4;

  private final GameManager match;
  private final FixedStepClock clock = new FixedStepClock(GdxConfig.SECONDS_PER_TICK);
  private final LawnGeometry geometry;

  private LawnRenderer lawnRenderer;
  private EntityRenderer entityRenderer;
  private StageRuleRenderer stageRuleRenderer;
  private CursorRenderer cursorRenderer;
  private HudStage hud;
  private final HudArt hudArt = new HudArt();
  private GameplayInputHandler input;
  private GdxGameActions actions;
  private boolean ended;
  private boolean paused;
  private int bonusScore;
  private int zombiesEating;
  private int projectilesFlying;
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

  private ConveyorRule belt() {
    return match == null || match.getSpecialStageRule() == null
        ? null : match.getSpecialStageRule().belt();
  }

  private BossStageRule bossRule() {
    return match != null && match.getSpecialStageRule() instanceof BossStageRule boss ? boss : null;
  }

  private void buildBar() {
    ConveyorRule belt = belt();
    if (belt != null) {
      hud.buildConveyorBar(belt, this::selectSeed, input::toggleShovel,
          input::togglePlantFood, this::togglePause);
      return;
    }
    List<PlantTemplate> deck = deckTemplates();
    hud.buildSeedBar(game.getUiSkin().get(), deck, this::selectSeed,
        input::toggleShovel, input::togglePlantFood, this::togglePause);
    input.setSeedOrder(deckNames(deck));
  }

  /**
   * Picks a seed up, and says so when it cannot be afforded yet.
   *
   * <p>The card already greys itself while the sun is short, but the doc wants an actual error on
   * the click; without one, a player clicking a dimmed card got a cursor holding a plant they were
   * not allowed to put down and no explanation until they clicked the lawn.
   */
  private void selectSeed(String plantType) {
    input.setSelectedPlantType(plantType);
    if (plantType == null || match == null || match.isFreePlanting()) {
      return;
    }
    PlantTemplate template = GameDataManager.plantRepository == null
        ? null : GameDataManager.plantRepository.find(plantType);
    if (template != null && match.getSunAmount() < template.cost) {
      hud.toast("Not enough sun for " + template.name + " (needs " + template.cost + ").");
    }
  }

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

  private boolean isBonus() {
    return match != null && match.isBonusMatch();
  }

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

  private void saveAndLeave() {
    // The lawn itself goes to Games.json; the account (coins, quests, progress) goes to the
    // server as it always did. A mini-game or bonus run declines the first part on its own.
    boolean stored = MatchSaveManager.save(match);
    if (stored) {
      hud.toast("Match saved -- resume it from the adventure map.");
    }
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

  private void showStageDialogue() {
    Dialogue.showConversation(hud.getStage(), game.getUiSkin().get(),
        Dialogue.stageConversation(seasonName(), playerName()), this::beginPlaying);
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
      text.append(ruleName(rule)).append('\n');
    }
    text.append("Defend your lawn -- don't let the zombies reach the house!");
    appendQuests(text);
    return text.toString();
  }

  /**
   * What this level's special rule is, in words.
   *
   * <p>The briefing used to print the rule's Java class name, so a player was told
   * "LoveYourPlantsRule is active this level".
   */
  private static String ruleName(SpecialStageRule rule) {
    if (rule instanceof ConveyorRule) {
      return "Conveyor belt: plants arrive on the belt, there is no seed bank.";
    }
    if (rule instanceof SaveOurSeedsRule) {
      return "Save Our Seeds: the marked plants must survive to the end.";
    }
    if (rule instanceof DeadLineRule) {
      return "Deadline: no zombie may cross the red line.";
    }
    if (rule instanceof TimedWarRule) {
      return "Timed battle: finish the objective before the clock runs out.";
    }
    if (rule instanceof LoveYourPlantsRule) {
      return "Don't lose your plants: lose too many and the level is over.";
    }
    if (rule instanceof PlantWhatYouGetRule) {
      return "Plant what you get: plant freely, then start the waves yourself.";
    }
    return "A special rule is active this level.";
  }

  /**
   * The quests this level counts towards, with their progress.
   *
   * <p>The doc wants the level's missions shown before it starts; the quest menu had them but the
   * level-start screen did not, so the player had to leave the map to find out what to aim for.
   */
  private static void appendQuests(StringBuilder text) {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null || user.getQuests() == null) {
      return;
    }
    List<Quest> open = new ArrayList<>();
    for (Quest quest : user.getQuests()) {
      if (!quest.isRewardClaimed() && open.size() < MAX_BRIEFING_QUESTS) {
        open.add(quest);
      }
    }
    if (open.isEmpty()) {
      return;
    }
    text.append("\n\nQuests in progress:");
    for (Quest quest : open) {
      text.append("\n  - ").append(quest.getTitle());
      if (quest.isCompleted()) {
        text.append("  (done -- claim it in the quest menu)");
      } else if (quest.getQuestTarget() > 0) {
        text.append(String.format("  (%d/%d)",
            (int) quest.getProgressOfQuest(), (int) quest.getQuestTarget()));
      }
    }
  }

  private void layout() {
    float[] box = LawnRenderer.lawnBounds(match);
    geometry.setBounds(box[0], box[1], box[2], box[3]);
  }

  @Override
  public void render(float delta) {
    if (!paused && !intro && match != null && match.isRunning()) {
      clock.update(delta * GameSettings.getGameSpeed(), actions::advanceOneTick);
    }
    float worldDelta = paused || intro ? 0f : delta;
    // The clock stops accumulating while paused, so its alpha is already frozen: publishing it
    // unconditionally leaves everything exactly where the last live frame drew it.
    context().setTickAlpha(clock.alpha());

    input.updateHover(Gdx.input.getX(), Gdx.input.getY());
    if (!paused && !intro) {
      input.collectSunUnderPointer();
    }

    context().applyCamera();
    lawnRenderer.render(context(), match, worldDelta);
    stageRuleRenderer.render(context(), match, worldDelta);
    entityRenderer.render(context(), match, worldDelta);
    if (!paused) {
      cursorRenderer.render(context(), match, worldDelta);
    }

    watchWaves();
    showPickups();
    showScoreNotices();
    listenForBites();
    listenForShots();
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
    hud.setSun(match.getSunAmount());
    hud.setObjective(objectiveText());
    if (paused) {
      hud.setStatus("paused");
      return;
    }
    hud.setStatus("plant food " + match.getPlantFoodCount() + nightNote() + hint());
  }

  /**
   * The bonus-game scoring patterns, as an on-screen alert.
   *
   * <p>They used to be a console println only, so the player earned MyoPoints for a multi-kill and
   * was never told it had happened.
   */
  private void showScoreNotices() {
    if (match == null || match.getScoreManager() == null) {
      return;
    }
    for (String notice : match.getScoreManager().drainPendingNotices()) {
      hud.alert(notice);
    }
  }

  private void showPickups() {
    if (match == null || match.getBoard() == null) {
      return;
    }
    for (String pickup : match.getBoard().drainPendingNotices()) {
      hud.toast(pickup);
    }
  }

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

  /**
   * The shot and the hit, the two sounds the doc asks for on a projectile.
   *
   * <p>A shot is a projectile that was not on the board last frame; the hit comes from the
   * renderer, which already works out that a projectile landed in order to draw the splat. Both
   * are counted rather than tracked per projectile, since the audio layer's own repeat guard
   * collapses a volley into one sound anyway.
   */
  private void listenForShots() {
    if (match == null || match.getBoard() == null || paused || intro) {
      return;
    }
    int flying = match.getBoard().getProjectiles().size();
    if (flying > projectilesFlying) {
      GameAudio.getInstance().play(GameAudio.Sfx.SHOOT);
    }
    projectilesFlying = flying;
    if (entityRenderer != null && entityRenderer.drainImpactCount() > 0) {
      GameAudio.getInstance().play(GameAudio.Sfx.EXPLODE);
    }
  }

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

  private String objectiveText() {
    if (isBonus()) {
      return "MyoPoints " + match.getScoreManager().getCurrentMatchScore()
          + "   -   survive for as long as you can";
    }
    SpecialStageRule rule = match.getSpecialStageRule();
    if (rule instanceof TimedWarRule timed) {
      // Both halves of the win condition, each marked done or not: the doc wants the state of a
      // timed stage's objectives visible, and a bare countdown does not say what still has to
      // happen for the level to be won.
      boolean clockDone = timed.remainingTicks() <= 0;
      boolean lawnClear = countZombies() == 0;
      return String.format("%s outlast the clock (%.1fs)      %s clear the lawn (%d left)",
          clockDone ? "[DONE]" : "[    ]", timed.remainingTicks() / 10.0,
          lawnClear ? "[DONE]" : "[    ]", countZombies());
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
    bonusScore = match.getScoreManager().getCurrentMatchScore();
    // Won or lost, this match is over: the resume button must not offer it again.
    MatchSaveManager.clear();
    MatchCompletion.apply(match);
    paused = false;
    if (game.getUiSkin().get() == null) {
      return;
    }
    boolean won = match.getMatchResult() != null && match.getMatchResult().isWon();
    if (won && match.getSpecialStageRule() instanceof BossStageRule) {
      Dialogue.show(hud.getStage(), game.getUiSkin().get(), Dialogue.CRAZY_DAVE,
          Dialogue.afterZomboss(seasonName()), () -> showOutcome(true));
      return;
    }
    showOutcome(won);
  }

  private void showOutcome(boolean won) {
    GameAudio.getInstance().play(won ? GameAudio.Sfx.WIN : GameAudio.Sfx.LOSE);
    Skin skin = game.getUiSkin().get();
    Table body = new Table();
    body.defaults().pad(2f);

    body.add(outcomeHeadline(skin, won)).padBottom(10f).row();

    body.add(summary(skin, won)).padBottom(4f).row();

    if (isBonus()) {
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
      if (won && result.getRewardCoins() > 0) {
        stats.add(statRow(skin, "Coins earned",
            new CountUpLabel(result.getRewardCoins(), skin, UiSkinProvider.LABEL_BIG_OUTLINE),
            UiSkinProvider.COIN_ICON)).row();
      }
    }
    return stats;
  }

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
