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

/**
 * The seed packets during a match: plant picture, sun cost, and whether it can be planted now.
 *
 * <p>The cards are {@link SeedCard}, shared with the almanac and the deck builder. Only the match
 * reading lives here: recharge uses the same numbers GamePlayController checks, so a card looks
 * ready exactly when the model would accept the plant.
 */
public final class SeedBar extends Table implements Disposable {

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
      add(card).width(96f).height(110f);
    }
  }

  /** Boosts bought on the selection screen, locked in when the match started. */
  private static boolean isBoosted(String plantName) {
    List<String> boosted = MatchSetup.getInstance().getBoostedPlants();
    return plantName != null && boosted != null
        && boosted.contains(plantName.toLowerCase().trim());
  }

  /** Repaints the cards from the live match. */
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
