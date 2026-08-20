package view.gdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import model.core.GameManager;
import model.game.plant.PlantParts.PlantLevel;
import model.game.plant.PlantParts.PlantTemplate;

/**
 * The seed packets: plant picture, sun cost, and whether it can be planted right now.
 *
 * <p>Recharge comes from the same numbers GamePlayController checks, so a card only looks ready
 * when the model would actually accept the plant.
 */
public final class SeedBar extends Table implements Disposable {

  private static final Color READY = new Color(1f, 1f, 1f, 1f);
  private static final Color RECHARGING = new Color(0.34f, 0.34f, 0.40f, 1f);
  private static final Color BROKE = new Color(0.85f, 0.75f, 0.45f, 1f);

  private final PlantArt plantArt = new PlantArt();
  private final HudArt hudArt = new HudArt();
  private final List<Card> cards = new ArrayList<>();
  private final Skin skin;

  public SeedBar(Skin skin, List<String> deck, List<PlantTemplate> templates,
      Consumer<String> onPick) {
    this.skin = skin;
    defaults().pad(3f);
    for (PlantTemplate template : templates) {
      Card card = new Card(template, onPick);
      cards.add(card);
      add(card).width(92f).height(108f);
    }
  }

  /** Repaints the cards from the live match. */
  public void update(GameManager match, String selected, ToIntFunction<String> levelOf) {
    for (Card card : cards) {
      card.refresh(match, selected, levelOf);
    }
  }

  @Override
  public void dispose() {
    plantArt.dispose();
    hudArt.dispose();
  }

  private final class Card extends Table {

    private final PlantTemplate template;
    private final Label costLabel;
    private final Label stateLabel;
    private final Image portrait;

    private Card(PlantTemplate template, Consumer<String> onPick) {
      this.template = template;
      setBackground(skin.getDrawable(UiSkinProvider.PANEL_BACKGROUND));
      pad(4f);

      TextureRegion art = plantArt.find(template.name);
      portrait = new Image(art);
      portrait.setScaling(Scaling.fit);
      if (art != null) {
        add(portrait).size(56f, 48f).colspan(2).row();
      } else {
        Label none = new Label("no art", skin, "secondary");
        none.setAlignment(Align.center);
        add(none).size(56f, 48f).colspan(2).row();
      }

      Label name = new Label(template.name, skin, "secondary");
      name.setAlignment(Align.center);
      name.setFontScale(0.85f);
      add(name).width(84f).colspan(2).row();

      costLabel = new Label(String.valueOf(template.cost), skin, UiSkinProvider.LABEL_MEDIUM);
      costLabel.setAlignment(Align.center);
      TextureRegion sun = hudArt.find("sun");
      if (sun != null) {
        Image sunIcon = new Image(sun);
        sunIcon.setScaling(Scaling.fit);
        add(sunIcon).size(18f, 18f).right();
        add(costLabel).left().padLeft(3f).row();
      } else {
        add(costLabel).width(84f).colspan(2).row();
      }

      stateLabel = new Label("", skin, UiSkinProvider.LABEL_MEDIUM);
      stateLabel.setAlignment(Align.center);
      add(stateLabel).width(84f).colspan(2);

      setTouchable(Touchable.enabled);
      addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          onPick.accept(template.name);
        }
      });
    }

    private void refresh(GameManager match, String selected, ToIntFunction<String> levelOf) {
      boolean chosen = template.name.equalsIgnoreCase(selected);
      setBackground(skin.getDrawable(
          chosen ? UiSkinProvider.DIALOG_BORDER : UiSkinProvider.PANEL_BACKGROUND));
      if (match == null) {
        return;
      }
      int recharge = Math.max(0, template.recharge
          + PlantLevel.cumulative(template, levelOf.applyAsInt(template.name))
              .getCooldownDeltaSeconds());
      int ticksLeft = match.ticksUntilPlantReady(template.name, recharge);
      boolean affordable = match.isFreePlanting() || match.getSunAmount() >= template.cost;

      if (ticksLeft > 0) {
        setColor(RECHARGING);
        stateLabel.setText(String.format("%.1fs", ticksLeft / 10.0));
      } else if (!affordable) {
        setColor(BROKE);
        stateLabel.setText("need sun");
      } else {
        setColor(READY);
        stateLabel.setText(chosen ? "selected" : "ready");
      }
    }
  }
}
