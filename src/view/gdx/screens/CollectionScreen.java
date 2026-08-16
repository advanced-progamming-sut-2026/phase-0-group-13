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
import model.account.User;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.zombie.ZombieParts.ZombieTemplate;
import model.game.zombie.ZombieParts.ZombieTypeResolver;
import view.gdx.core.PvzGdxGame;
import view.gdx.ui.PlantArt;
import view.gdx.ui.UiSkinProvider;


/**
 * Graphical Collection: the plant and zombie almanac.
 *
 * <p>Shows what CollectionMenuController prints, with the same unlock checks, so an unseen zombie
 * stays behind "???" here too.
 */
public final class CollectionScreen extends MenuScreen {

  private static final String ZOMBIE_PREFIX = "zombie_";
  private static final int COLUMNS = 4;

  private final PlantArt plantArt = new PlantArt();
  private Table content;
  private boolean showingPlants = true;
  private String selected;

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

    Table split = new Table();
    split.add(gridPane(user)).width(760f).height(468f).padRight(18f).top();
    split.add(detailsPane(user)).width(452f).height(468f).top();
    content.add(split).row();
  }

  private TextButton tab(String text, boolean active, boolean plants) {
    if (active) {
      // Not disabled on purpose - greying it out would make it look like the inactive tab.
      return new TextButton(text, skin, UiSkinProvider.BUTTON_GREEN);
    }
    return button(text, UiSkinProvider.BUTTON_BROWN, () -> {
      showingPlants = plants;
      selected = null;

      refresh();
    });
  }

  private ScrollPane gridPane(User user) {
    Table grid = panel();
    grid.top();
    grid.defaults().pad(8f).width(168f).height(150f);

    int column = 0;
    if (showingPlants) {
      for (PlantTemplate template : plants()) {
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

  private Table plantCard(User user, PlantTemplate template) {
    boolean unlocked = user.hasUnlockedPlant(template.name);
    // The terminal almanac names locked plants too, so the card can show it.
    String state = unlocked ? "Lv " + user.getPlantLevel(template.name) : "locked";
    return card(template.name, state, template.name, unlocked, plantArt.find(template.name));
  }

  private Table zombieCard(User user, ZombieTemplate template) {
    boolean seen = hasSeen(user, template);
    return card(seen ? template.getName() : "???", seen ? "seen" : "not encountered",
            template.getName(), seen, null);
  }

  /** One grid card: art on top, name and state under it. The whole card is clickable. */
  private Table card(String name, String state, String key, boolean enabled, TextureRegion art) {
    Table card = new Table();
    card.setBackground(skin.getDrawable(enabled && key.equals(selected)
            ? UiSkinProvider.DIALOG_BORDER : UiSkinProvider.PANEL_BACKGROUND));
    card.pad(6f);

    if (art != null) {
      Image image = new Image(art);
      image.setScaling(Scaling.fit);
      card.add(image).size(96f, 66f).padBottom(2f).row();
    } else {
      Label none = new Label(enabled ? "art unavailable" : "", skin, "secondary");
      none.setAlignment(Align.center);
      card.add(none).size(96f, 66f).padBottom(2f).row();
    }

    Label title = new Label(name, skin, UiSkinProvider.LABEL_MEDIUM);
    title.setWrap(true);
    title.setAlignment(Align.center);
    card.add(title).width(150f).row();
    card.add(new Label(state, skin, "secondary")).row();

    if (enabled) {
      card.setTouchable(Touchable.enabled);
      card.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          selected = key;
          refresh();
        }
      });
    } else {
      // Dim enough to read as locked, not so far that the name goes unreadable.
      card.getColor().a = 0.72f;
    }
    return card;
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

    details.add(new Label(template.getName(), skin, UiSkinProvider.LABEL_BIG))
        .left()
        .padBottom(8f)
        .row();
    addSprite(details, "zombies", template.getName(), "textures/zombies/");

    field(details, "type", String.valueOf(ZombieTypeResolver.resolve(template)));
    field(details, "health", String.valueOf(template.getBaseHp()));
    field(details, "speed", String.valueOf(template.getBaseSpeed()));
    field(details, "eat dps", String.valueOf(template.getEatDps()));
    field(details, "abilities", template.getStatsSummary());
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

  /** Sprite for this entity, or a short "no art" note when PlantArt has none. */
  private void addSprite(Table details, String section, String name, String dir) {
    TextureRegion region = "plants".equals(section) ? plantArt.find(name) : null;

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
            ? List.of() : GameDataManager.zombieRepository.getAll();
  }

  private boolean hasSeen(User user, ZombieTemplate template) {
    return user.getUnlockedZombies().contains(ZOMBIE_PREFIX + template.getName().toLowerCase());
  }

  @Override
  public void dispose() {
    super.dispose();
    plantArt.dispose();
  }
}
