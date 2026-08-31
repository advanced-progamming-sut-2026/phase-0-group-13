package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
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


public final class ShopScreen extends MenuScreen {

  private static final float SHELF_WIDTH = 1020f;
  private static final float CARD_WIDTH = 318f;
  private static final float CARD_HEIGHT = 240f;
  private static final int CARDS_PER_ROW = 3;
  private static final float RIBBON_CLEARANCE = 46f;
  /** Gap under a shelf heading; wide enough that a promo ribbon never touches the words. */
  private static final float SHELF_HEADING_GAP = 14f;

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
    header.add(wallet(user)).left().expandX();
    header.defaults().pad(8f).width(190f).height(58f);
    header.add(button("Greenhouse", UiSkinProvider.BUTTON_GREEN,
        () -> go(new GreenhouseScreen(game))));
    header.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())));
    content.add(header).width(SHELF_WIDTH).padBottom(6f).row();

    Table list = new Table();
    list.top();

    List<ShopItem> daily = shop.getDailyTimeProducts(user);
    list.add(shelfHeading("Daily deal")).left().padBottom(SHELF_HEADING_GAP).row();
    if (daily.isEmpty()) {
      list.add(new Label("Nothing on offer today.", skin, "secondary")).left().padBottom(10f).row();
    } else {
      list.add(shelf(user, daily)).left().padBottom(16f).row();
    }

    list.add(shelfHeading("Always in stock")).left().padTop(8f).padBottom(SHELF_HEADING_GAP).row();
    list.add(shelf(user, shop.getAllTimeProducts())).left().row();

    ScrollPane scroll = new ScrollPane(list, skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);

    Table frame = panel();
    frame.add(scroll).width(SHELF_WIDTH).height(452f);
    content.add(frame).row();
  }

  private Label shelfHeading(String text) {
    return new Label(text, skin, UiSkinProvider.LABEL_BIG_OUTLINE);
  }

  private Table shelf(User user, List<ShopItem> items) {
    Table shelf = new Table();
    shelf.left().top();
    int column = 0;
    for (ShopItem item : items) {
      shelf.add(itemCard(user, item)).width(CARD_WIDTH).height(CARD_HEIGHT).pad(6f);
      if (++column % CARDS_PER_ROW == 0) {
        shelf.row();
      }
    }
    return shelf;
  }

  private Table wallet(User user) {
    Table wallet = new Table();
    wallet.add(purse(UiSkinProvider.COIN_ICON, user.getCoins())).padRight(12f);
    wallet.add(purse(UiSkinProvider.GEM_ICON, user.getDiamonds()));
    return wallet;
  }

  private Table purse(String icon, int amount) {
    Table purse = new Table();
    purse.setBackground(skin.getDrawable(UiSkinProvider.COUNTER_PLATE));
    purse.pad(6f, 14f, 6f, 18f);
    purse.add(new Image(skin.getDrawable(icon))).size(38f).padRight(10f);
    purse.add(new Label(String.valueOf(amount), skin, UiSkinProvider.LABEL_BIG_OUTLINE));
    return purse;
  }

  private Table itemCard(User user, ShopItem item) {
    boolean affordable = canAfford(user, item);

    Table card = new Table();
    card.setBackground(skin.getDrawable(cardArt(item)));
    card.pad(item.isDaily() ? RIBBON_CLEARANCE : 12f, 16f, 16f, 16f);
    card.top();

    card.add(new Image(skin.getDrawable(itemIcon(item)))).size(54f).padTop(4f).padBottom(2f).row();
    card.add(new Label(displayName(item), skin, UiSkinProvider.LABEL_MEDIUM)).padBottom(2f).row();

    Label blurb = new Label(description(item), skin, "secondary");
    blurb.setWrap(true);
    blurb.setAlignment(Align.center);
    card.add(blurb).width(CARD_WIDTH - 44f).growY().row();

    card.add(priceTag(item, affordable)).padBottom(8f).row();
    card.add(affordable
            ? button("Buy", UiSkinProvider.BUTTON_GREEN, () -> openPurchase(user, item))
            : disabledButton(item))
        .width(CARD_WIDTH - 60f)
        .height(54f);

    if (!item.isDaily()) {
      return card;
    }
    Stack stack = new Stack();
    stack.add(card);
    stack.add(ribbon("TODAY ONLY"));
    Table wrapper = new Table();
    wrapper.add(stack).grow();
    return wrapper;
  }

  private Table ribbon(String text) {
    Table holder = new Table();
    holder.top().padTop(2f);
    Table banner = new Table();
    banner.setBackground(skin.getDrawable(UiSkinProvider.PROMO_RIBBON));
    banner.add(new Label(text, skin, "promo_ribbon"));
    holder.add(banner).width(150f).height(38f);
    return holder;
  }

  private TextButton disabledButton(ShopItem item) {
    TextButton button = new TextButton(
        item.getCurrencyType() == CurrencyType.COIN ? "Need coins" : "Need gems",
        skin, UiSkinProvider.BUTTON_BROWN);
    button.setDisabled(true);
    button.getColor().a = 0.55f;
    return button;
  }

  private static boolean canAfford(User user, ShopItem item) {
    int purse = item.getCurrencyType() == CurrencyType.COIN
        ? user.getCoins() : user.getDiamonds();
    return purse >= item.getPrice();
  }

  private static String cardArt(ShopItem item) {
    if (item.isDaily()) {
      return "image_ui_cards_store_store_bundle_card";
    }
    return switch (item.getCategory()) {
      case POT -> "image_ui_cards_store_store_sprout_card";
      case PLANT_FOOD -> "image_ui_cards_store_store_upgrade_card";
      case CURRENCY_CONVERSION -> "image_ui_cards_store_store_coin_card";
      default -> "image_ui_cards_store_store_plant_card";
    };
  }

  private static String itemIcon(ShopItem item) {
    return switch (item.getCategory()) {
      case POT -> "image_ui_hud_ingame_sprout_icon_noplus";
      case PLANT_FOOD -> "image_ui_almanac_plant_food_stat_icon";
      case CURRENCY_CONVERSION -> "image_ui_coins_stack_5";
      default -> "image_ui_gems_stack_3";
    };
  }

  private Table priceTag(ShopItem item) {
    return priceTag(item, true);
  }

  private Table priceTag(ShopItem item, boolean affordable) {
    Table price = new Table();
    String icon = item.getCurrencyType() == CurrencyType.COIN
            ? UiSkinProvider.COIN_ICON : UiSkinProvider.GEM_ICON;
    price.add(new Image(skin.getDrawable(icon))).size(30f).padRight(8f);
    Label amount = new Label(String.valueOf(item.getPrice()), skin,
        UiSkinProvider.LABEL_MEDIUM_OUTLINE);
    if (!affordable) {
      amount.getColor().set(1f, 0.55f, 0.5f, 1f);
    }
    price.add(amount);
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
    runAsync(
        () -> {
          UserManager.getInstance().updateCurrentUserGameState();
          return null;
        },
        ignored -> {},
        e -> toast(e.getMessage()));
  }
}
