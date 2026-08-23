package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import data.GameDataManager;
import data.persistence.UserManager;
import java.util.List;
import model.Result;
import model.account.User;
import model.core.GameManager;
import model.core.GameSession;
import model.core.MatchLauncher;
import model.core.MatchSetup;
import model.game.minigame.SpecialStageRule;
import model.game.plant.PlantParts.PlantTemplate;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.PlantArt;
import view.gdx.ui.UiSkinProvider;


/**
 * Graphical "choose your plants" screen, shown before a level that isn't a Conveyor Belt stage.
 *
 * <p>Same underlying model calls as the terminal's PlantSelectionMenuController: clicking a card
 * toggles it in/out of {@code User.getSelectedDeck()} via addToDeck/removeFromDeck, so cooldowns,
 * the seed bank cap and per-stage plant locks (a level's {@link SpecialStageRule}) behave exactly
 * the same in both builds. The layout mirrors CollectionScreen's card grid, just with a picked
 * state and a cost line instead of an almanac entry.
 */
public final class PlantSelectionScreen extends MenuScreen {

  private static final int COLUMNS = 4;

  private final int stage;
  private final int level;
  private final PlantArt plantArt = new PlantArt();
  private Table content;

  public PlantSelectionScreen(PvzGdxGame game, int stage, int level) {
    super(game);
    this.stage = stage;
    this.level = level;
  }

  @Override
  protected String title() {
    return "Level " + level + " - Choose your plants";
  }

  @Override
  protected Screen backTarget() {
    return new AdventureScreen(game, stage);
  }

  @Override
  protected String backgroundAtlasPath() {
    return "textures/environment/" + worldAtlas(stage);
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

    SpecialStageRule rule = MatchLauncher.selectionRule();

    Table grid = panel();
    grid.top();
    grid.defaults().pad(8f).width(168f).height(172f);
    int column = 0;
    for (PlantTemplate template : plants()) {
      grid.add(plantCard(user, template, rule));
      if (++column % COLUMNS == 0) {
        grid.row();
      }
    }
    ScrollPane scroll = new ScrollPane(grid, skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    content.add(scroll).growX().maxHeight(440f).row();

    content.add(new Label(
            "Selected: " + user.getSelectedDeck().size() + " / " + User.MAX_DECK_SLOTS,
            skin, UiSkinProvider.LABEL_MEDIUM))
        .padTop(10f)
        .row();

    Table actionsRow = new Table();
    actionsRow.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())))
        .width(200f)
        .padRight(12f);
    actionsRow.add(button("Start battle", UiSkinProvider.BUTTON_GREEN, this::startBattle))
        .width(240f);
    content.add(actionsRow).padTop(8f);
  }

  private Table plantCard(User user, PlantTemplate template, SpecialStageRule rule) {
    boolean unlocked = user.hasUnlockedPlant(template.name);
    boolean allowed = unlocked && (rule == null || rule.isPlantAllowed(template.name));
    boolean selected = user.getSelectedDeck().contains(template.name);
    boolean boosted = user.isPlantBoosted(template.name);

    Table card = new Table();
    card.setBackground(skin.getDrawable(
        selected ? UiSkinProvider.DIALOG_BORDER : UiSkinProvider.PANEL_BACKGROUND));
    card.pad(6f);

    TextureRegion art = plantArt.find(template.name);
    if (art != null) {
      Image image = new Image(art);
      image.setScaling(Scaling.fit);
      card.add(image).size(96f, 66f).padBottom(2f).row();
    } else {
      Label none = new Label(unlocked ? "no art" : "???", skin, "secondary");
      none.setAlignment(Align.center);
      card.add(none).size(96f, 66f).padBottom(2f).row();
    }

    Label name = new Label(template.name, skin, UiSkinProvider.LABEL_MEDIUM);
    name.setWrap(true);
    name.setAlignment(Align.center);
    card.add(name).width(150f).row();

    String state = !unlocked ? "locked"
        : !allowed ? "locked this stage"
        : selected ? (boosted ? "selected (boosted)" : "selected")
        : "cost " + template.cost;
    card.add(new Label(state, skin, "secondary")).row();

    if (allowed) {
      card.setTouchable(Touchable.enabled);
      card.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          toggle(user, template.name);
        }
      });
      if (selected && !boosted) {
        TextButton boost = new TextButton("Boost (2 gems)", skin, UiSkinProvider.BUTTON_BROWN);
        boost.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            event.stop();
            Result result = user.boostPlant(template.name);
            toast(result.message());
            refresh();
          }
        });
        card.add(boost).padTop(4f);
      }
    } else {
      card.getColor().a = 0.6f;
    }
    return card;
  }

  private void toggle(User user, String plantName) {
    boolean selected = user.getSelectedDeck().contains(plantName);
    Result result = selected ? user.removeFromDeck(plantName) : user.addToDeck(plantName);
    if (!result.success()) {
      toast(result.message());
    }
    refresh();
  }

  private void startBattle() {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      toast("error: no user logged in");
      return;
    }

    SpecialStageRule rule = MatchLauncher.selectionRule();
    int selectable = (int) user.getUnlockedPlants().stream()
        .filter(name -> rule == null || rule.isPlantAllowed(name))
        .count();
    int required = Math.min(User.MIN_DECK_SLOTS, selectable);
    int deckSize = user.getSelectedDeck().size();
    if (deckSize < required) {
      toast("Pick at least " + required + " plants (" + deckSize + "/" + required + ")");
      return;
    }

    MatchSetup.getInstance().setSelectedPlants(user.getSelectedDeck());
    MatchSetup.getInstance().setBoostedPlants(user.getBoostedPlants());
    MatchSetup.getInstance().setDifficultyLevel(user.getDifficultyLevel());

    MatchLauncher.launch();
    GameManager started = GameSession.getActiveGame();
    if (started == null) {
      toast("could not start the level");
      return;
    }
    go(new GameplayScreen(game, started, null));
  }

  private List<PlantTemplate> plants() {
    return GameDataManager.plantRepository == null
        ? List.of() : GameDataManager.plantRepository.getAll();
  }

  /** Same stage-to-atlas mapping AdventureScreen uses for its own backgrounds. */
  private static String worldAtlas(int stage) {
    return switch (stage) {
      case 1 -> "ancientegyptseason.atlas";
      case 2 -> "frostbitecavesseason.atlas";
      case 3 -> "bigwavebeachseason.atlas";
      default -> "darkagesseason.atlas";
    };
  }

  @Override
  public void dispose() {
    super.dispose();
    plantArt.dispose();
  }
}
