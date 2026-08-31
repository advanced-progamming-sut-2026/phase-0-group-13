package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
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
import view.gdx.ui.HudArt;
import view.gdx.ui.PlantArt;
import view.gdx.ui.Popup;
import view.gdx.ui.UiSkinProvider;


public final class GreenhouseScreen extends MenuScreen {

  private static final int COLUMNS = 5;
  private static final int ROWS = 4;
  private static final String POT_ITEM = "pot_1";
  private static final int POT_PRICE = 2000;
  private static final long MILLIS_PER_HOUR = 60L * 60 * 1000;
  private static final int MARIGOLD_COINS = 500;

  private static final float POT_WIDTH = 116f;
  private static final float POT_HEIGHT = 74f;
  private static final float PLANT_LIFT = 24f;
  private static final float POT_BUTTON_HEIGHT = 30f;

  private final PlantArt art = new PlantArt();
  private final HudArt hudArt = new HudArt();
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

    Table grid = new Table();
    grid.defaults().pad(4f).width(POT_WIDTH + 14f).top();
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        int index = row * COLUMNS + col;
        grid.add(potCard(user, greenhouse.getPotAt(index), index));
      }
      grid.row();
    }
    content.add(grid).row();
  }

  private Table potCard(User user, Pot pot, int index) {
    boolean locked = pot == null || !pot.isUnlocked();
    boolean ready = !locked && !pot.isEmpty() && pot.isFullyGrown();

    Table card = new Table();
    card.defaults().pad(1f);
    card.add(potVisual(pot, locked, ready)).size(POT_WIDTH, POT_HEIGHT).row();

    if (locked) {
      card.add(smallButton("Buy  " + POT_PRICE, UiSkinProvider.BUTTON_BROWN,
          () -> buyPot(user))).width(POT_WIDTH).height(POT_BUTTON_HEIGHT);
      return card;
    }
    if (pot.isEmpty()) {
      card.add(smallButton("Plant seed", UiSkinProvider.BUTTON_GREEN,
          () -> openEmptyPot(user, index))).width(POT_WIDTH).height(POT_BUTTON_HEIGHT);
      return card;
    }

    card.add(new Label(ready ? "READY" : remaining(pot), skin, "secondary")).row();
    card.add(smallButton(ready ? "Collect" : pot.getPlantedSeedId(),
            ready ? UiSkinProvider.BUTTON_GREEN : UiSkinProvider.BUTTON_BROWN,
            () -> openGrowingPot(pot, index)))
        .width(POT_WIDTH).height(POT_BUTTON_HEIGHT);
    return card;
  }

  private TextButton smallButton(String text, String style, Runnable action) {
    TextButton button = button(text, style, action);
    button.getLabel().setFontScale(UiSkinProvider.fontScale(0.82f));
    return button;
  }

  private Stack potVisual(Pot pot, boolean locked, boolean ready) {
    Stack stack = new Stack();
    stack.add(bottomAligned(hudArt.find("potshadow"), POT_WIDTH * 0.70f, 16f, 0f, 0.55f));

    if (!locked && pot != null && !pot.isEmpty()) {
      TextureRegion sprite = art.find(pot.getPlantedSeedId());
      if (sprite != null) {
        stack.add(bottomAligned(sprite, 52f, 46f, PLANT_LIFT, 1f));
      }
    }

    TextureRegion potArt = hudArt.find(ready ? "potgold" : "pot");
    stack.add(bottomAligned(potArt, POT_WIDTH * 0.80f, 46f, 0f, locked ? 0.5f : 1f));

    if (locked) {
      stack.add(bottomAligned(hudArt.find("potlocked"), 22f, 28f, 18f, 1f));
    } else if (ready) {
      stack.add(bottomAligned(hudArt.find("potwater"), 17f, 26f, 38f, 1f));
    }
    return stack;
  }

  private Table bottomAligned(TextureRegion region, float width, float height, float lift,
      float alpha) {
    Table holder = new Table();
    holder.bottom();
    if (region == null) {
      return holder;
    }
    Image image = new Image(region);
    image.setScaling(Scaling.fit);
    image.getColor().a = alpha;
    holder.add(image).size(width, height).padBottom(lift);
    return holder;
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

  private void buyPot(User user) {
    Result result = shop.buyItem(user, POT_ITEM, 1, null);
    toast(result.message());
    if (result.success()) {
      saveState();
    }
    refresh();
  }

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
    runAsync(
        () -> {
          UserManager.getInstance().updateCurrentUserGameState();
          return null;
        },
        ignored -> {},
        e -> toast(e.getMessage()));
  }

  @Override
  public void dispose() {
    super.dispose();
    art.dispose();
    hudArt.dispose();
  }
}
