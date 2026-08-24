package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Scaling;
import data.persistence.UserManager;
import model.Result;
import model.account.User;
import model.environment.greenhouse.GreenHouse;
import model.environment.greenhouse.Pot;
import model.game.shop.Shop;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.PlantArt;
import view.gdx.ui.Popup;
import view.gdx.ui.UiSkinProvider;


/**
 * Graphical Greenhouse: the 5x4 pot grid the Phase 1 menu addresses by (x, y).
 *
 * <p>Every action goes through the GreenHouse model, so a pot allows exactly what the terminal
 * menu allows. Tapping a pot opens a popup with its state and whatever action it supports right
 * now, which keeps the grid itself readable.
 */
public final class GreenhouseScreen extends MenuScreen {

  private static final int COLUMNS = 5;
  private static final int ROWS = 4;
  /** Same price the shop's pot_1 item charges. */
  private static final String POT_ITEM = "pot_1";
  private static final int POT_PRICE = 2000;
  private static final long MILLIS_PER_HOUR = 60L * 60 * 1000;
  private static final int MARIGOLD_COINS = 500;

  private final PlantArt art = new PlantArt();
  private final Shop shop = new Shop();
  private Table content;

  public GreenhouseScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  protected String title() {
    return "Greenhouse";
  }

  @Override
  protected Screen backTarget() {
    return new MainMenuScreen(game);
  }

  @Override
  protected String backgroundAtlasPath() {
    return "textures/environment/bigwavebeachseason.atlas";
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

    GreenHouse greenhouse = user.getGreenHouse();

    Table header = new Table();
    header.defaults().pad(8f);
    header.add(new Label(greenhouse.getUnlockedPotsCount() + " / " + greenhouse.getMaxCapacity()
            + " pots unlocked", skin, UiSkinProvider.LABEL_MEDIUM)).padRight(30f);
    header.add(button("Shop", UiSkinProvider.BUTTON_GREEN, () -> go(new ShopScreen(game))))
        .width(200f)
        .height(62f);
    header.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())))
        .width(200f)
        .height(62f);
    content.add(header).padBottom(10f).row();

    // No panel behind the grid - each pot has its own, so they read as separate tiles.
    Table grid = new Table();
    grid.defaults().pad(8f).width(184f).height(118f);
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        int index = row * COLUMNS + col;
        grid.add(potCard(user, greenhouse.getPotAt(index), index));
      }
      grid.row();
    }
    content.add(grid).row();
  }

  /** One pot: locked, empty, growing or ready. */
  private Table potCard(User user, Pot pot, int index) {
    Table card = new Table();
    card.setBackground(skin.getDrawable(UiSkinProvider.PANEL_BACKGROUND));
    card.pad(8f);
    card.defaults().pad(2f);

    if (pot == null || !pot.isUnlocked()) {
      card.add(button("Buy pot\n" + POT_PRICE + " coins", UiSkinProvider.BUTTON_BROWN,
          () -> buyPot(user))).grow();
      return card;
    }

    if (pot.isEmpty()) {
      TextButton plant = button("Plant seed", UiSkinProvider.BUTTON_GREEN,
          () -> openEmptyPot(user, index));
      plant.getLabel().setWrap(true);
      card.add(plant).grow();
      return card;
    }

    String seed = pot.getPlantedSeedId();
    TextureRegion sprite = art.find(seed);
    if (sprite != null) {
      Image image = new Image(sprite);
      image.setScaling(Scaling.fit);
      card.add(image).size(70f, 40f).row();
    }
    card.add(new Label(seed, skin, "secondary")).row();

    boolean ready = pot.isFullyGrown();
    card.add(new Label(ready ? "READY" : remaining(pot), skin, UiSkinProvider.LABEL_MEDIUM)).row();
    TextButton open = button(ready ? "Collect" : "Details",
            ready ? UiSkinProvider.BUTTON_GREEN : UiSkinProvider.BUTTON_BROWN,
            () -> openGrowingPot(pot, index));
    card.add(open).growX().height(34f);
    return card;
  }

  private void openEmptyPot(User user, int index) {
    Table body = new Table();
    body.add(new Label("Pot " + (index + 1) + " is empty.\nPlanting picks a seed the same way\n"
            + "the greenhouse always has.", skin, UiSkinProvider.LABEL_MEDIUM)).row();

    Popup.show(stage, skin, "Empty pot", body, "Plant", () -> {
      Result result = user.getGreenHouse().plantSeed(index, user);
      toast(result.message());
      saveState();
      refresh();
    });
  }

  private void openGrowingPot(Pot pot, int index) {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      return;
    }

    Table body = new Table();
    body.defaults().pad(4f);
    TextureRegion sprite = art.find(pot.getPlantedSeedId());
    if (sprite != null) {
      Image image = new Image(sprite);
      image.setScaling(Scaling.fit);
      body.add(image).size(220f, 160f).padBottom(10f).row();
    }
    body.add(new Label(pot.getPlantedSeedId(), skin, UiSkinProvider.LABEL_MEDIUM)).row();
    body.add(new Label(pot.isFullyGrown()
            ? "Fully grown and ready to collect."
            : "Ready in " + remaining(pot)
              + "   (" + Math.round(pot.getGrowthProgress() * 100) + "% grown)",
            skin, UiSkinProvider.LABEL_MEDIUM)).row();

    if (pot.isFullyGrown()) {
      Popup.show(stage, skin, "Pot " + (index + 1), body, "Collect", () -> collect(user, index));
      return;
    }
    int diamonds = diamondCost(pot);
    body.add(new Label("Growing it now costs " + diamonds + " diamond(s).",
        skin, UiSkinProvider.LABEL_MEDIUM))
        .row();
    Popup.show(stage, skin, "Pot " + (index + 1), body,
        "Grow for " + diamonds, () -> forceGrow(user, index));
  }

  /** The shop owns the price and the unlock; this only reports the Result. */
  private void buyPot(User user) {
    Result result = shop.buyItem(user, POT_ITEM, 1, null);
    toast(result.message());
    if (result.success()) {
      saveState();
    }
    refresh();
  }

  /** Same charge the typed "grow" command makes: one diamond per hour still to run. */
  private static int diamondCost(Pot pot) {
    return (int) Math.ceil(pot.getRemainingGrowTime() / (double) MILLIS_PER_HOUR);
  }

  private void forceGrow(User user, int index) {
    Pot pot = user.getGreenHouse().getPotAt(index);
    if (pot == null || pot.isEmpty() || pot.isFullyGrown()) {
      toast("error: invalid pot for growing");
      return;
    }
    int diamonds = diamondCost(pot);
    if (user.getDiamonds() < diamonds) {
      toast("error: not enough diamonds. You need " + diamonds + " diamonds.");
      return;
    }
    user.addDiamonds(-diamonds);
    Result result = user.getGreenHouse().forceGrow(index);
    toast(result.message() + " (-" + diamonds + " diamonds)");
    saveState();
    refresh();
  }

  /** Harvesting pays out, same as the typed menu, and says what it paid. */
  private void collect(User user, int index) {
    Result result = user.getGreenHouse().collectSeed(index);
    if (!result.success()) {
      toast(result.message());
      refresh();
      return;
    }
    String seed = String.valueOf(result.getObject());
    if ("marigold".equalsIgnoreCase(seed)) {
      user.addCoins(MARIGOLD_COINS);
      toast("Collected a Marigold! +" + MARIGOLD_COINS + " coins.");
    } else {
      Result boost = user.addFreeBoost(seed);
      toast("Harvested " + seed + ": " + boost.message());
    }
    saveState();
    refresh();
  }

  private String remaining(Pot pot) {
    long minutes = pot.getRemainingGrowTime() / 60000L;
    if (minutes >= 60) {
      return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
    return minutes + "m";
  }

  private void saveState() {
    try {
      UserManager.getInstance().updateCurrentUserGameState();
    } catch (Exception e) {
      toast(e.getMessage());
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    art.dispose();
  }
}
