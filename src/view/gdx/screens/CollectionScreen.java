package view.gdx.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Scaling;
import controller.MainMenuSubControllers.GameMenuSubControllers.CollectionMenuController;
import data.GameDataManager;
import data.persistence.UserManager;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import model.account.User;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.zombie.ZombieParts.ZombieTemplate;
import model.game.zombie.ZombieParts.ZombieTypeResolver;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.HudArt;
import view.gdx.ui.PlantArt;
import view.gdx.ui.SeedCard;
import view.gdx.ui.UiSkinProvider;
import view.gdx.ui.ZombieArt;


public final class CollectionScreen extends MenuScreen {

  private static final String ZOMBIE_TOUGHNESS_ICON =
      "image_ui_almanac_zombies_zombietoughness_icon";
  private static final String ZOMBIE_SPEED_ICON = "image_ui_almanac_zombies_zombiespeed_icon";

  private static final String ZOMBIE_PREFIX = "zombie_";
  private static final int COLUMNS = 4;
  private static final String ALL = "All";
  private static final String LOCKED = "Locked";
  private static final String UPGRADABLE = "Upgradable";
  private static final com.badlogic.gdx.graphics.Color LOCKED_TINT =
      new com.badlogic.gdx.graphics.Color(1f, 1f, 1f, 0.6f);

  private final PlantArt plantArt = new PlantArt();
  private final ZombieArt zombieArt = new ZombieArt();
  private final HudArt hudArt = new HudArt();
  private final CollectionMenuController collection = new CollectionMenuController();
  private Table content;
  private boolean showingPlants = true;
  private String selected;
  private String filter = ALL;

  public CollectionScreen(PvzGdxGame game) {
    super(game);
  }

  @Override
  protected String title() {
    return "Collection";
  }

  @Override
  protected Screen backTarget() {
    return new MainMenuScreen(game);
  }

  @Override
  protected String backgroundAtlasPath() {
    return "textures/environment/ancientegyptseason.atlas";
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

    Table tabs = new Table();
    tabs.defaults().pad(8f).width(230f).height(66f);
    tabs.add(tab("Plants", showingPlants, true));
    tabs.add(tab("Zombies", !showingPlants, false));
    tabs.add(button("Back", UiSkinProvider.BUTTON_BROWN, () -> go(backTarget())));
    content.add(tabs).padBottom(10f).row();

    if (showingPlants) {
      content.add(filters()).padBottom(8f).row();
    }

    float paneHeight = showingPlants ? 396f : 468f;
    Table split = new Table();
    split.add(gridPane(user)).width(760f).height(paneHeight).padRight(18f).top();
    split.add(detailsPane(user)).width(452f).height(paneHeight).top();
    content.add(split).row();
  }

  private TextButton tab(String text, boolean active, boolean plants) {
    if (active) {
      return new TextButton(text, skin, UiSkinProvider.BUTTON_GREEN);
    }
    return button(text, UiSkinProvider.BUTTON_BROWN, () -> {
      showingPlants = plants;
      selected = null;

      refresh();
    });
  }

  private Table filters() {
    Table row = new Table();
    row.defaults().pad(3f).width(160f).height(44f);
    int added = 0;
    for (String name : filterNames()) {
      String style = name.equals(filter)
              ? UiSkinProvider.BUTTON_GREEN : UiSkinProvider.BUTTON_BROWN;
      row.add(button(name, style, () -> {
        filter = name;
        refresh();
      }));
      if (++added % 6 == 0) {
        row.row();
      }
    }
    return row;
  }

  private List<String> filterNames() {
    Set<String> names = new LinkedHashSet<>();
    names.add(ALL);
    names.add(LOCKED);
    names.add(UPGRADABLE);
    for (PlantTemplate template : plants()) {
      if (template.category != null && !template.category.isBlank()) {
        names.add(template.category);
      }
    }
    return new ArrayList<>(names);
  }

  private boolean matchesFilter(User user, PlantTemplate template) {
    switch (filter) {
      case ALL:
        return true;
      case LOCKED:
        return !user.hasUnlockedPlant(template.name);
      case UPGRADABLE:
        return isUpgradable(user, template);
      default:
        return template.category != null && template.category.equalsIgnoreCase(filter);
    }
  }

  private boolean isUpgradable(User user, PlantTemplate template) {
    if (!user.hasUnlockedPlant(template.name)) {
      return false;
    }
    int level = user.getPlantLevel(template.name);
    return level < CollectionMenuController.MAX_PLANT_LEVEL
            && packets(user, template) >= CollectionMenuController.UPGRADE_SEED_COST * level
            && user.getCoins() >= CollectionMenuController.UPGRADE_COIN_COST * level;
  }

  private static int packets(User user, PlantTemplate template) {
    return user.getInventory().getItemCount("seed_" + template.name.toLowerCase().trim());
  }

  private static String packetLine(User user, PlantTemplate template) {
    int level = user.getPlantLevel(template.name);
    int have = packets(user, template);
    return level >= CollectionMenuController.MAX_PLANT_LEVEL
            ? "Lv " + level + "   max   " + have + " pkt"
            : "Lv " + level + "   " + have + "/"
              + CollectionMenuController.UPGRADE_SEED_COST * level + " pkt";
  }

  private ScrollPane gridPane(User user) {
    Table grid = panel();
    grid.top();
    grid.defaults().pad(8f).width(168f).height(180f);

    int column = 0;
    if (showingPlants) {
      for (PlantTemplate template : plants()) {
        if (!matchesFilter(user, template)) {
          continue;
        }
        grid.add(plantCard(user, template));
        if (++column % COLUMNS == 0) {
          grid.row();
        }
      }
    } else {
      for (ZombieTemplate template : zombies()) {
        grid.add(zombieCard(user, template));
        if (++column % COLUMNS == 0) {
          grid.row();
        }
      }
    }

    ScrollPane scroll = new ScrollPane(grid, skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    return scroll;
  }

  private SeedCard plantCard(User user, PlantTemplate template) {
    boolean unlocked = user.hasUnlockedPlant(template.name);
    SeedCard card = new SeedCard(skin, SeedCard.Size.FULL, template.name, template.name,
            plantArt.find(template.name), hudArt, this::select)
            .withCost(template.cost);
    card.setStatus(unlocked ? packetLine(user, template)
            : "locked - " + CollectionMenuController.PURCHASE_COST_COINS + " coins");
    card.setBoosted(user.isPlantBoosted(template.name));
    if (!unlocked) {
      card.setTint(LOCKED_TINT);
    }
    card.setSelected(template.name.equals(selected));
    card.withCardArt(UiSkinProvider.ALMANAC_PLANT_CARD);
    if (!unlocked) {
      card.withLock();
    }
    return card;
  }

  private SeedCard zombieCard(User user, ZombieTemplate template) {
    boolean seen = hasSeen(user, template);
    SeedCard card = new SeedCard(skin, SeedCard.Size.FULL, template.getName(),
            seen ? template.getDisplayName() : "???", seen ? zombieArt.find(template.getName()) : null,
            hudArt, this::select);
    card.setStatus(seen ? "seen" : "not encountered");
    card.setEnabled(seen);
    card.setSelected(seen && template.getName().equals(selected));
    card.withCardArt(UiSkinProvider.ALMANAC_ZOMBIE_CARD);
    if (!seen) {
      card.withLock();
    }
    return card;
  }

  private void select(String key) {
    selected = key;
    refresh();
  }

  private ScrollPane detailsPane(User user) {
    Table details = panel();
    details.top();

    if (selected == null) {
      details.add(new Label("Pick an entry to see its details.", skin,
              UiSkinProvider.LABEL_MEDIUM)).left();
    } else if (showingPlants) {
      plantDetails(details, user);
    } else {
      zombieDetails(details, user);
    }

    ScrollPane scroll = new ScrollPane(details, skin);
    scroll.setFadeScrollBars(false);
    scroll.setScrollingDisabled(true, false);
    return scroll;
  }

  private void plantDetails(Table details, User user) {
    PlantTemplate template = GameDataManager.plantRepository == null
            ? null : GameDataManager.plantRepository.find(selected);
    if (template == null) {
      return;
    }

    details.add(new Label(template.name, skin, UiSkinProvider.LABEL_BIG)).left().padBottom(8f).row();
    addSprite(details, "plants", template.name, "textures/plants/");

    boolean unlocked = user.hasUnlockedPlant(template.name);
    field(details, "status", unlocked
            ? "unlocked (level " + user.getPlantLevel(template.name) + ")" : "locked");
    field(details, "category", template.category);
    field(details, "tags", template.tags);
    field(details, "sun cost", String.valueOf(template.cost));
    field(details, "health", String.valueOf(template.baseHp));
    field(details, "damage", template.damage);
    field(details, "recharge", template.recharge + "s");
    field(details, "interval", template.actionInterval);
    field(details, "ability", template.baseAbility);
    field(details, "plant food", template.plantFoodEffect);

    if (!unlocked) {
      field(details, "unlock price", CollectionMenuController.PURCHASE_COST_COINS + " coins");
      details.add(button("Buy this plant", UiSkinProvider.BUTTON_GREEN,
              () -> purchase(user, template))).left().width(260f).height(58f).padTop(12f).row();
      return;
    }

    int level = user.getPlantLevel(template.name);
    field(details, "seed packets", packetLine(user, template));
    if (level >= CollectionMenuController.MAX_PLANT_LEVEL) {
      return;
    }
    field(details, "next upgrade", CollectionMenuController.UPGRADE_SEED_COST * level
            + " packets and " + CollectionMenuController.UPGRADE_COIN_COST * level + " coins");
    details.add(button("Upgrade", UiSkinProvider.BUTTON_PURPLE, () -> upgrade(user, template)))
            .left().width(260f).height(58f).padTop(12f).row();
  }

  private void purchase(User user, PlantTemplate template) {
    if (user.getCoins() < CollectionMenuController.PURCHASE_COST_COINS) {
      toast("error: not enough coins (need " + CollectionMenuController.PURCHASE_COST_COINS
              + ", you have " + user.getCoins() + ")");
      return;
    }
    collection.handleinput("menu collection purchase-plant -p " + template.name);
    toast(user.hasUnlockedPlant(template.name)
            ? "Unlocked " + template.name + "!"
            : "error: could not buy " + template.name);
    refresh();
  }

  private void upgrade(User user, PlantTemplate template) {
    int before = user.getPlantLevel(template.name);
    collection.handleinput("menu collection upgrade-plant -p " + template.name);
    int after = user.getPlantLevel(template.name);
    toast(after > before
            ? template.name + " upgraded to level " + after + "!"
            : "error: not enough seed packets or coins to upgrade " + template.name);
    refresh();
  }

  private void zombieDetails(Table details, User user) {
    ZombieTemplate template = GameDataManager.zombieRepository == null
            ? null : GameDataManager.zombieRepository.find(selected);
    if (template == null) {
      return;
    }
    if (!hasSeen(user, template)) {
      details.add(new Label("You haven't encountered this zombie yet.", skin,
              UiSkinProvider.LABEL_MEDIUM)).left();
      return;
    }

    details.add(new Label(template.getDisplayName(), skin, UiSkinProvider.LABEL_BIG))
        .left()
        .padBottom(8f)
        .row();
    addSprite(details, "zombies", template.getName(), "textures/zombies/");

    field(details, "type", String.valueOf(ZombieTypeResolver.resolve(template)));
    details.add(statLine(template)).left().padTop(8f).row();
    field(details, "eat dps", String.valueOf(template.getEatDps()));
    field(details, "abilities", template.getStatsSummary());
  }

  private Table statLine(ZombieTemplate template) {
    Table stats = new Table();
    stats.left();
    stats.add(stat(ZOMBIE_TOUGHNESS_ICON, String.valueOf(template.getBaseHp()))).padRight(26f);
    stats.add(stat(ZOMBIE_SPEED_ICON, String.valueOf(template.getBaseSpeed())));
    return stats;
  }

  private Table stat(String icon, String value) {
    Table cell = new Table();
    cell.add(new Image(skin.getDrawable(icon))).size(34f).padRight(8f);
    cell.add(new Label(value, skin, UiSkinProvider.LABEL_MEDIUM));
    return cell;
  }

  private void field(Table details, String label, String value) {
    if (value == null || value.isBlank() || "-".equals(value)) {
      return;
    }
    details.add(new Label(label, skin, "secondary")).left().padTop(7f).row();
    Label text = new Label(value, skin, UiSkinProvider.LABEL_MEDIUM);
    text.setWrap(true);
    details.add(text).left().width(384f).padTop(1f).row();
  }

  private void addSprite(Table details, String section, String name, String dir) {
    TextureRegion region =
        "plants".equals(section) ? plantArt.find(name) : zombieArt.find(name);

    if (region == null) {
      details.add(new Label("no verified artwork", skin, "secondary")).left().padBottom(10f).row();
      return;
    }
    Image art = new Image(region);
    art.setScaling(Scaling.fit);
    details.add(art).size(300f, 200f).padBottom(14f).row();
  }

  private List<PlantTemplate> plants() {
    return GameDataManager.plantRepository == null
            ? List.of() : GameDataManager.plantRepository.getAll();
  }

  private List<ZombieTemplate> zombies() {
    return GameDataManager.zombieRepository == null
            ? List.of() : GameDataManager.zombieRepository.getAlmanacEntries();
  }

  private boolean hasSeen(User user, ZombieTemplate template) {
    return user.getUnlockedZombies().contains(ZOMBIE_PREFIX + template.getName().toLowerCase());
  }

  @Override
  public void dispose() {
    super.dispose();
    plantArt.dispose();
    zombieArt.dispose();
    hudArt.dispose();
  }
}
