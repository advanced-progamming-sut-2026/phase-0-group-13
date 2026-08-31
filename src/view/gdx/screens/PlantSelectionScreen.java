package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import data.GameDataManager;
import data.persistence.UserManager;
import java.util.List;
import model.Result;
import model.account.User;
import model.core.BonusGameLauncher;
import model.core.GameManager;
import model.core.GameSession;
import model.core.MatchLauncher;
import model.core.MatchSetup;
import model.game.minigame.SpecialStageRule;
import model.game.plant.PlantParts.PlantTemplate;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.HudArt;
import view.gdx.ui.PlantArt;
import view.gdx.ui.Popup;
import view.gdx.ui.SeedCard;
import view.gdx.ui.UiSkinProvider;

/**
 * The deck builder between the Adventure map and the lawn.
 *
 * <p>No rules of its own: User owns the eight-slot limit and the boost price, MatchLauncher says
 * which plants a stage locks out and when Start is allowed. Same calls the typed
 * PlantSelectionMenuController makes. Cards are {@link SeedCard}.
 */
public final class PlantSelectionScreen extends MenuScreen {

  private static final int GRID_COLUMNS = 6;

  private final int chapter;
  private final boolean bonus;
  private final PlantArt plantArt = new PlantArt();
  private final HudArt hudArt = new HudArt();

  private Table content;

  public PlantSelectionScreen(PvzGdxGame game, int chapter) {
    this(game, chapter, false);
  }

  /**
   * Deck building for the daily bonus run.
   *
   * <p>Same screen because the choosing is the same: the only differences are where Back goes and
   * which launcher Start hands off to. {@link MatchSetup#setBonusRun()} has already told
   * {@link MatchLauncher#selectionRule()} that no stage is locking plants out, so the almanac needs
   * no special case. Egypt's backdrop stands in, the bonus run having no season of its own.
   */
  public static PlantSelectionScreen forBonusGame(PvzGdxGame game) {
    MatchSetup.getInstance().setBonusRun();
    return new PlantSelectionScreen(game, 1, true);
  }

  private PlantSelectionScreen(PvzGdxGame game, int chapter, boolean bonus) {
    super(game);
    this.chapter = chapter;
    this.bonus = bonus;
  }

  @Override
  protected String title() {
    return bonus ? "Bonus Game  -  choose your plants" : "Choose your plants";
  }

  @Override
  protected Screen backTarget() {
    // For a chapter, back into its level grid rather than the chapter list already stepped past.
    return bonus ? new MainMenuScreen(game) : new AdventureScreen(game, chapter);
  }

  @Override
  protected String backgroundAtlasPath() {
    return "textures/environment/" + switch (chapter) {
      case 1 -> "ancientegyptseason.atlas";
      case 2 -> "frostbitecavesseason.atlas";
      case 3 -> "bigwavebeachseason.atlas";
      default -> "darkagesseason.atlas";
    };
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

    content.add(seedBank(user)).growX().padBottom(10f).row();
    content.add(almanac(user)).growX().maxHeight(340f).row();
    content.add(footer(user)).padTop(8f);
  }

  private Table seedBank(User user) {
    Table panel = panel();
    List<String> deck = user.getSelectedDeck();
    int required = MatchLauncher.requiredDeckSlots(user);

    panel.add(new Label("Seed bank  " + deck.size() + " / " + User.MAX_DECK_SLOTS
            + "   (at least " + required + " to start)", skin, UiSkinProvider.LABEL_MEDIUM))
            .left().padBottom(8f).row();

    Table slots = new Table();
    // compact: eight menu-sized cards plus the almanac do not fit on a 720-tall screen
    slots.defaults().pad(4f).width(96f).height(110f);
    for (String plant : deck) {
      slots.add(chosenCard(user, plant));
    }
    for (int i = deck.size(); i < User.MAX_DECK_SLOTS; i++) {
      slots.add(emptySlot());
    }
    panel.add(slots).row();
    return panel;
  }

  /** Tapping a card in the bank offers boost or remove. */
  private SeedCard chosenCard(User user, String plant) {
    PlantTemplate template = template(plant);
    SeedCard card = new SeedCard(skin, SeedCard.Size.COMPACT, plant, displayName(plant, template),
            plantArt.find(plant), hudArt, key -> openSlotActions(user, key));
    if (template != null) {
      card.withCost(template.cost);
    }
    card.setStatus(upgradeLine(user, plant));
    card.setBoosted(user.isPlantBoosted(plant));
    card.setSelected(true);
    return card;
  }

  private Table emptySlot() {
    Table slot = new Table();
    slot.setBackground(skin.getDrawable(UiSkinProvider.PANEL_BACKGROUND));
    Label label = new Label("empty", skin, "secondary");
    slot.add(label);
    slot.getColor().a = 0.55f;
    return slot;
  }

  /** Everything the player owns, taken or not. */
  private ScrollPane almanac(User user) {
    SpecialStageRule rule = MatchLauncher.selectionRule();
    Table grid = panel();
    grid.top();
    grid.defaults().pad(6f).width(168f).height(180f);

    int column = 0;
    for (String plant : user.getUnlockedPlants()) {
      grid.add(almanacCard(user, plant, rule));
      if (++column % GRID_COLUMNS == 0) {
        grid.row();
      }
    }

    ScrollPane scroll = new ScrollPane(grid, skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    return scroll;
  }

  private SeedCard almanacCard(User user, String plant, SpecialStageRule rule) {
    PlantTemplate template = template(plant);
    boolean allowed = rule == null || rule.isPlantAllowed(plant);
    boolean chosen = user.getSelectedDeck().contains(plant);

    SeedCard card = new SeedCard(skin, SeedCard.Size.FULL, plant, displayName(plant, template),
            plantArt.find(plant), hudArt, key -> toggle(user, key, rule));
    if (template != null) {
      card.withCost(template.cost);
    }
    card.setStatus(allowed ? upgradeLine(user, plant) : "locked in this stage");
    card.setBoosted(user.isPlantBoosted(plant));
    card.setEnabled(allowed);
    card.setSelected(chosen);
    return card;
  }

  /** Level plus packets held. The upgrade price stays in CollectionMenuController. */
  private String upgradeLine(User user, String plant) {
    int packets = user.getInventory().getItemCount("seed_" + plant.toLowerCase().trim());
    return "Lv " + user.getPlantLevel(plant) + "   " + packets + " pkt";
  }

  /** Click to add, click again to remove. */
  private void toggle(User user, String plant, SpecialStageRule rule) {
    if (rule != null && !rule.isPlantAllowed(plant)) {
      toast("error: " + plant + " is locked in this stage");
      return;
    }
    Result result = user.getSelectedDeck().contains(plant)
            ? user.removeFromDeck(plant)
            : user.addToDeck(plant);
    toast(result.message());
    refresh();
  }

  private void openSlotActions(User user, String plant) {
    Table body = new Table();
    body.add(new Label(user.isPlantBoosted(plant)
            ? plant + " is already boosted."
            : "Boosting costs 2 diamonds and fires its\nPlant Food effect the moment it is planted.",
            skin, UiSkinProvider.LABEL_MEDIUM)).row();
    body.add(new Label(upgradeOfferLine(user, plant), skin, UiSkinProvider.LABEL_MEDIUM))
            .padTop(8f);

    Popup.show(stage, skin, plant, body,
            new Popup.Choice("Boost", UiSkinProvider.BUTTON_PURPLE, () -> boost(user, plant)),
            new Popup.Choice("Upgrade", UiSkinProvider.BUTTON_GREEN, () -> upgrade(user, plant)),
            new Popup.Choice("Remove", UiSkinProvider.BUTTON_BROWN,
                () -> toggle(user, plant, MatchLauncher.selectionRule())),
            new Popup.Choice("Keep", UiSkinProvider.BUTTON_GREEN, null));
  }

  private String upgradeOfferLine(User user, String plant) {
    int level = user.getPlantLevel(plant);
    if (level >= User.MAX_PLANT_LEVEL) {
      return plant + " is at the maximum level.";
    }
    return "Upgrade to level " + (level + 1) + " costs "
            + user.upgradeSeedCost(plant) + " packets and "
            + user.upgradeCoinCost(plant) + " coins.";
  }

  private void boost(User user, String plant) {
    Result result = user.boostPlant(plant);
    toast(result.message());
    if (result.success()) {
      save();
    }
    refresh();
  }

  private void upgrade(User user, String plant) {
    Result result = user.upgradePlant(plant);
    toast(result.message());
    if (result.success()) {
      save();
    }
    refresh();
  }

  private Table footer(User user) {
    Table row = new Table();
    row.defaults().pad(6f).width(240f).height(64f);
    row.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())));
    row.add(button("Clear", UiSkinProvider.BUTTON_PURPLE, () -> {
      user.clearDeck();
      refresh();
    }));

    int required = MatchLauncher.requiredDeckSlots(user);
    if (user.getSelectedDeck().size() < required) {
      TextButton start = new TextButton("Start", skin, UiSkinProvider.BUTTON_GREEN);
      start.setDisabled(true);
      start.getColor().a = 0.6f;
      row.add(start);
    } else {
      row.add(button("Start", UiSkinProvider.BUTTON_GREEN, () -> start(user)));
    }
    return row;
  }

  /** Same hand-off the typed menu does: lock the setup in, then let MatchLauncher build it. */
  private void start(User user) {
    int required = MatchLauncher.requiredDeckSlots(user);
    if (user.getSelectedDeck().size() < required) {
      toast("error: select at least " + required + " plants before starting ("
              + user.getSelectedDeck().size() + "/" + required + ")");
      return;
    }
    save();

    MatchSetup.getInstance().setSelectedPlants(user.getSelectedDeck());
    MatchSetup.getInstance().setBoostedPlants(user.getBoostedPlants());
    MatchSetup.getInstance().setDifficultyLevel(user.getDifficultyLevel());

    if (bonus) {
      BonusGameLauncher.launch();
    } else {
      MatchLauncher.launch();
    }
    GameManager started = GameSession.getActiveGame();
    if (started == null) {
      toast(bonus ? "could not start the bonus run" : "could not start the level");
      return;
    }
    go(new GameplayScreen(game, started));
  }

  private void save() {
    runAsync(
        () -> {
          UserManager.getInstance().updateCurrentUserGameState();
          return null;
        },
        ignored -> {},
        e -> toast(e.getMessage()));
  }

  /** The deck stores lowercase keys, the almanac has the real spelling. */
  private static String displayName(String plant, PlantTemplate template) {
    return template == null ? plant : template.name;
  }

  private static PlantTemplate template(String plant) {
    return GameDataManager.plantRepository == null
            ? null : GameDataManager.plantRepository.find(plant);
  }

  @Override
  public void dispose() {
    super.dispose();
    plantArt.dispose();
    hudArt.dispose();
  }
}
