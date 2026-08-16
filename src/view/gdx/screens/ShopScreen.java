package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import data.persistence.UserManager;
import java.util.List;
import model.Result;
import model.account.User;
import model.enums.CurrencyType;
import model.game.shop.Shop;
import model.game.shop.ShopItem;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.Popup;
import view.gdx.ui.UiSkinProvider;


/**
 * Graphical Shop over the existing Shop model.
 *
 * <p>Items, prices and purchase rules all come from Shop; Buy just calls buyItem and shows the
 * Result, so there's no second copy of the validation here. One Shop per screen, same as
 * ShopMenuController builds one per controller.
 */
public final class ShopScreen extends MenuScreen {

  private final Shop shop = new Shop();
  private Table content;

  public ShopScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  protected String title() {
    return "Shop";
  }

  @Override
  protected Screen backTarget() {
    return new MainMenuScreen(game);
  }

  @Override
  protected String backgroundAtlasPath() {
    return "textures/environment/darkagesseason.atlas";
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
    shop.refreshDailyDealsIfNeeded(user);

    Table header = new Table();
    header.defaults().pad(8f).width(210f).height(62f);
    header.add(button("Greenhouse", UiSkinProvider.BUTTON_GREEN,
        () -> go(new GreenhouseScreen(game))));
    header.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())));
    content.add(header).padBottom(10f).row();

    Table list = panel();
    list.top();
    list.defaults().padBottom(12f);

    list.add(new Label("Daily deal", skin, UiSkinProvider.LABEL_BIG)).left().row();
    List<ShopItem> daily = shop.getDailyTimeProducts(user);
    if (daily.isEmpty()) {
      list.add(new Label("Nothing on offer today.", skin, "secondary")).left().row();
    }
    for (ShopItem item : daily) {
      list.add(itemCard(user, item)).growX().row();
    }

    list.add(new Label("Always in stock", skin, UiSkinProvider.LABEL_BIG))
        .left()
        .padTop(14f)
        .row();
    for (ShopItem item : shop.getAllTimeProducts()) {
      list.add(itemCard(user, item)).growX().row();
    }

    ScrollPane scroll = new ScrollPane(list, skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    content.add(scroll).width(1050f).height(470f).row();
  }

  private Table itemCard(User user, ShopItem item) {
    Table card = new Table();
    card.setBackground(skin.getDrawable(UiSkinProvider.PANEL_BACKGROUND));
    card.pad(14f);
    card.left();

    Table text = new Table();
    text.left();
    text.add(new Label(displayName(item), skin, UiSkinProvider.LABEL_MEDIUM)).left().row();
    Label blurb = new Label(description(item), skin, "secondary");
    blurb.setWrap(true);
    text.add(blurb).left().width(560f).row();
    if (item.isDaily()) {
      text.add(new Label("today only - one per day", skin, "secondary")).left().row();
    }
    card.add(text).growX();

    card.add(priceTag(item)).right().padRight(18f);
    card.add(button("Buy", UiSkinProvider.BUTTON_GREEN, () -> openPurchase(user, item)))
        .width(170f)
        .height(62f);
    return card;
  }

  private Table priceTag(ShopItem item) {
    Table price = new Table();
    String icon = item.getCurrencyType() == CurrencyType.COIN
            ? UiSkinProvider.COIN_ICON : UiSkinProvider.GEM_ICON;
    price.add(new Image(skin.getDrawable(icon))).size(34f).padRight(8f);
    price.add(new Label(String.valueOf(item.getPrice()), skin, UiSkinProvider.LABEL_MEDIUM));
    return price;
  }

  private void openPurchase(User user, ShopItem item) {
    Table body = new Table();
    body.defaults().pad(5f);
    body.add(new Label(displayName(item), skin, UiSkinProvider.LABEL_MEDIUM)).row();
    Label blurb = new Label(description(item), skin, "secondary");
    blurb.setWrap(true);
    body.add(blurb).width(430f).row();
    body.add(priceTag(item)).padTop(10f).row();

    // Shop refuses a custom seed without a plant, so ask for one here.
    SelectBox<String> plantPicker = null;
    if (needsPlantChoice(item)) {
      List<String> owned = user.getUnlockedPlants();
      if (owned.isEmpty()) {
        body.add(new Label("You have no unlocked plants to pick.", skin, "secondary")).row();
      } else {
        plantPicker = new SelectBox<>(skin);
        plantPicker.setItems(owned.toArray(new String[0]));
        body.add(new Label("plant", skin, "secondary")).padTop(10f).row();
        body.add(plantPicker).width(340f).height(52f).row();
      }
    }

    final SelectBox<String> picker = plantPicker;
    Popup.show(stage, skin, "Purchase confirmation", body, "Buy", () -> {
      String plantType = picker == null ? null : picker.getSelected();
      Result result = shop.buyItem(user, item.getId(), 1, plantType);
      toast(result.message());
      if (result.success()) {
        saveState();
      }
      refresh();
    });
  }

  private boolean needsPlantChoice(ShopItem item) {
    return item.getCategory() == model.enums.ItemCategory.CUSTOM_SEED;
  }

  /** Readable names for the model's item ids. */
  private String displayName(ShopItem item) {
    return switch (item.getCategory()) {
      case POT -> "Greenhouse pot";
      case PLANT_FOOD -> "Plant food";
      case RANDOM_SEED -> item.isDaily() ? "Daily seed pack" : "Random seed pack";
      case CUSTOM_SEED -> "Seed pack of your choice";
      case CURRENCY_CONVERSION -> "Diamonds to coins";
      default -> item.getId();
    };
  }

  /** Mirrors what Shop.applyPurchaseEffect does for each category. */
  private String description(ShopItem item) {
    return switch (item.getCategory()) {
      case POT -> "Unlocks the next pot in your greenhouse.";
      case PLANT_FOOD -> "Adds one plant food. You can hold three at a time.";
      case RANDOM_SEED -> "10 seeds for one of the plants you already own.";
      case CUSTOM_SEED -> "10 seeds for a plant you pick.";
      case CURRENCY_CONVERSION -> "Trades diamonds for 500 coins.";
      default -> "";
    };
  }

  private void saveState() {
    try {
      UserManager.getInstance().updateCurrentUserGameState();
    } catch (Exception e) {
      toast(e.getMessage());
    }
  }
}
