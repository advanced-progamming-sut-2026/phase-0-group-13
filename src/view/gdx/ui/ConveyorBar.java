package view.gdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import java.util.function.Consumer;
import model.game.minigame.ConveyorRule;

public final class ConveyorBar extends Table implements Disposable {

  private static final Color READY = new Color(1f, 1f, 1f, 1f);
  private static final Color WAITING = new Color(0.4f, 0.4f, 0.46f, 1f);

  private final Skin skin;
  private final ConveyorRule rule;
  private final Consumer<String> onPick;
  private final PlantArt plantArt = new PlantArt();
  private final HudArt hudArt = new HudArt();

  private String shown = "";

  public ConveyorBar(Skin skin, ConveyorRule rule, Consumer<String> onPick) {
    this.skin = skin;
    this.rule = rule;
    this.onPick = onPick;
    defaults().pad(3f);
    rebuild();
  }

  public void update() {
    String ready = rule.peekReadyPlant();
    String key = ready == null ? "" : ready;
    if (!key.equals(shown)) {
      rebuild();
    }
  }

  private void rebuild() {
    clear();
    String ready = rule.peekReadyPlant();
    shown = ready == null ? "" : ready;

    add(new Label("belt", skin, "secondary")).padRight(6f);
    if (rule.getBeltPlants().isEmpty()) {
      add(new Label("nothing on the belt", skin, "secondary"));
      return;
    }
    for (String plant : rule.getBeltPlants()) {
      boolean available = plant.equalsIgnoreCase(shown);
      SeedCard card = new SeedCard(skin, SeedCard.Size.COMPACT, plant, plant,
          plantArt.find(plant), hudArt, available ? onPick : null);
      card.setStatus(available ? "free" : "queued");
      card.setSelected(available);
      card.setTint(available ? READY : WAITING);
      add(card).width(SeedBar.CARD_WIDTH).height(SeedBar.CARD_HEIGHT);
    }
  }

  @Override
  public void dispose() {
    plantArt.dispose();
    hudArt.dispose();
  }
}
