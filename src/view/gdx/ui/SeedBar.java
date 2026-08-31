package view.gdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import model.core.GameManager;
import model.core.MatchSetup;
import model.game.plant.PlantParts.PlantLevel;
import model.game.plant.PlantParts.PlantTemplate;

public final class SeedBar extends Table implements Disposable {

  /** Named, not just a literal on the add() call below, so HudStage can size its own row against
   * it -- the tools beside this bar must never end up taller than the cards they sit next to.
   *
   * <p>Wide enough that the longest name in the roster ("Cabbage-pult") stays on one line. It
   * used to be 12px narrower, which was a hair under what the name needed: the label wrapped to
   * two lines and the second line pushed the cost and the "need sun" face out through the bottom
   * of the card. Eight of these plus the tool buttons still leave room at 1280 wide. */
  public static final float CARD_WIDTH = 108f;
  public static final float CARD_HEIGHT = 110f;

  private static final Color READY = new Color(1f, 1f, 1f, 1f);
  private static final Color RECHARGING = new Color(0.34f, 0.34f, 0.40f, 1f);
  private static final Color BROKE = new Color(0.85f, 0.75f, 0.45f, 1f);

  private final PlantArt plantArt = new PlantArt();
  private final HudArt hudArt = new HudArt();
  private final List<PlantTemplate> templates = new ArrayList<>();
  private final List<SeedCard> cards = new ArrayList<>();

  public SeedBar(Skin skin, List<PlantTemplate> templates, Consumer<String> onPick) {
    defaults().pad(3f);
    for (PlantTemplate template : templates) {
      SeedCard card = new SeedCard(skin, SeedCard.Size.COMPACT, template.name, template.name,
          plantArt.find(template.name), hudArt, onPick)
          .withCost(template.cost);
      card.setBoosted(isBoosted(template.name));
      this.templates.add(template);
      cards.add(card);
      add(card).width(CARD_WIDTH).height(CARD_HEIGHT);
    }
  }

  private static boolean isBoosted(String plantName) {
    List<String> boosted = MatchSetup.getInstance().getBoostedPlants();
    return plantName != null && boosted != null
        && boosted.contains(plantName.toLowerCase().trim());
  }

  public void update(GameManager match, String selected, ToIntFunction<String> levelOf) {
    if (match == null) {
      return;
    }
    for (int i = 0; i < cards.size(); i++) {
      refresh(cards.get(i), templates.get(i), match, selected, levelOf);
    }
  }

  private void refresh(SeedCard card, PlantTemplate template, GameManager match, String selected,
      ToIntFunction<String> levelOf) {
    card.setSelected(template.name.equalsIgnoreCase(selected));

    int recharge = Math.max(0, template.recharge
        + PlantLevel.cumulative(template, levelOf.applyAsInt(template.name))
            .getCooldownDeltaSeconds());
    int ticksLeft = match.ticksUntilPlantReady(template.name, recharge);
    boolean affordable = match.isFreePlanting() || match.getSunAmount() >= template.cost;

    if (ticksLeft > 0) {
      card.setTint(RECHARGING);
      card.setStatus(String.format("%.1fs", ticksLeft / 10.0));
    } else if (!affordable) {
      card.setTint(BROKE);
      card.setStatus("need sun");
    } else {
      card.setTint(READY);
      card.setStatus(card.isSelected() ? "selected" : "ready");
    }
  }

  @Override
  public void dispose() {
    plantArt.dispose();
    hudArt.dispose();
  }
}
