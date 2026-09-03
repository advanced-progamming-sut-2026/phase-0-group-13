package view.gdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import model.game.minigame.ConveyorRule;

/**
 * The belt that hands you plants on a conveyor stage.
 *
 * <p>The rule still decides everything that matters: what has been delivered, and what may be
 * planted. All this does is carry the cards on a belt that is always running instead of standing
 * them in a table. A delivery runs the belt on until the plant being offered has reached the head;
 * whatever passes the near end doing so comes back on at the far end, and the rest close the gap.
 */
public final class ConveyorBar extends WidgetGroup implements Disposable {

  private static final float SLOT_GAP = 5f;
  private static final float TRACK_PAD = 7f;

  /** Belt speed in slots a second: quick enough to look driven, slow enough to follow. */
  private static final float SLOTS_PER_SECOND = 2.2f;
  private static final float TREAD_SPACING = 24f;
  private static final float TREAD_WIDTH = 9f;

  private static final Color READY = new Color(1f, 1f, 1f, 1f);
  private static final Color TRACK = new Color(0.09f, 0.09f, 0.11f, 0.8f);
  private static final Color TREAD = new Color(0.34f, 0.35f, 0.4f, 0.75f);

  /** One card riding the belt, with where it is now and the slot it is heading for. */
  private static final class Riding {
    private final SeedCard card;
    private final String plant;
    private float x;
    private int slot;

    private Riding(SeedCard card, String plant, float x, int slot) {
      this.card = card;
      this.plant = plant;
      this.x = x;
      this.slot = slot;
    }
  }

  private final Skin skin;
  private final ConveyorRule rule;
  private final Consumer<String> onPick;
  private final PlantArt plantArt = new PlantArt();
  private final HudArt hudArt = new HudArt();
  private final List<Riding> riding = new ArrayList<>();
  private final Label empty;
  private final Drawable track;
  private final Drawable tread;

  private String shown = "";
  private float treadOffset;

  public ConveyorBar(Skin skin, ConveyorRule rule, Consumer<String> onPick) {
    this.skin = skin;
    this.rule = rule;
    this.onPick = onPick;
    this.empty = new Label("nothing on the belt", skin, "secondary");
    this.track = tinted(skin, TRACK);
    this.tread = tinted(skin, TREAD);
    load();
  }

  /** The belt is drawn from the skin's plain pixel; a skin without one simply has no track. */
  private static Drawable tinted(Skin skin, Color color) {
    try {
      return skin.newDrawable(UiSkinProvider.WHITE_PIXEL, color);
    } catch (RuntimeException missing) {
      return null;
    }
  }

  /** Kept for the HUD, which pokes the belt once a frame; the riding itself happens in act. */
  public void update() {
    String key = String.join("|", rule.getDeliveredPlants());
    if (!key.equals(shown)) {
      shown = key;
      load();
    }
    paint();
  }

  private void load() {
    List<Float> wasAt = new ArrayList<>();
    for (Riding rider : riding) {
      wasAt.add(rider.x);
    }
    clearChildren();
    riding.clear();
    // The cards the belt has actually handed over. This used to be getBeltPlants() -- the whole
    // roster the stage draws from -- so the player saw every plant in the level riding the belt
    // with all but one greyed out, whether or not it had ever been delivered.
    List<String> plants = rule.getDeliveredPlants();
    if (plants.isEmpty()) {
      addActor(empty);
      return;
    }
    for (int i = 0; i < plants.size(); i++) {
      String plant = plants.get(i);
      SeedCard card = new SeedCard(skin, SeedCard.Size.COMPACT, plant, plant,
          plantArt.find(plant), hudArt, onPick);
      // The belt's cards carry the same sun cost the seed bank's do; the doc asks for the belt to
      // have the same capabilities as the normal seed list.
      model.game.plant.PlantParts.PlantTemplate template =
          data.GameDataManager.plantRepository == null
              ? null : data.GameDataManager.plantRepository.find(plant);
      if (template != null) {
        card.withCost(template.cost);
      }
      card.setSize(SeedBar.CARD_WIDTH, SeedBar.CARD_HEIGHT);
      addActor(card);
      // Cards already on the belt keep the screen position they had, so a delivery slides the new
      // card on from the right instead of the whole belt jumping a slot.
      float from = i < wasAt.size() ? wasAt.get(i) : slotX(plants.size());
      riding.add(new Riding(card, plant, from, i));
    }
    shown = String.join("|", plants);
    paint();
  }

  private void paint() {
    for (Riding rider : riding) {
      rider.card.setStatus("free");
      rider.card.setEnabled(true);
      rider.card.setTint(READY);
    }
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    float travel = delta * SLOTS_PER_SECOND * step();
    treadOffset = (treadOffset + travel) % TREAD_SPACING;
    for (Riding rider : riding) {
      float target = slotX(rider.slot);
      rider.x = rider.x > target ? Math.max(target, rider.x - travel) : target;
      rider.card.setPosition(rider.x, TRACK_PAD);
    }
  }

  @Override
  public void draw(Batch batch, float parentAlpha) {
    if (track != null) {
      drawBelt(batch);
    }
    super.draw(batch, parentAlpha);
  }

  /** The running belt under the cards: a dark track with treads sliding along it. */
  private void drawBelt(Batch batch) {
    float width = getWidth();
    float height = getHeight();
    track.draw(batch, getX(), getY(), width, height);
    for (float x = -treadOffset; x < width; x += TREAD_SPACING) {
      float left = Math.max(0f, x);
      float span = Math.min(TREAD_WIDTH - (left - x), width - left);
      if (span > 0f) {
        tread.draw(batch, getX() + left, getY() + 2f, span, height - 4f);
      }
    }
  }

  private int slots() {
    return riding.size();
  }

  private float step() {
    return SeedBar.CARD_WIDTH + SLOT_GAP;
  }

  private float slotX(int slot) {
    return TRACK_PAD + slot * step();
  }

  @Override
  public float getPrefWidth() {
    return riding.isEmpty() ? empty.getPrefWidth() : slotX(riding.size()) - SLOT_GAP + TRACK_PAD;
  }

  @Override
  public float getPrefHeight() {
    return riding.isEmpty() ? empty.getPrefHeight() : SeedBar.CARD_HEIGHT + TRACK_PAD * 2f;
  }

  @Override
  public void dispose() {
    plantArt.dispose();
    hudArt.dispose();
  }
}
