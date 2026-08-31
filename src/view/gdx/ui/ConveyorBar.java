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
  private static final Color WAITING = new Color(0.4f, 0.4f, 0.46f, 1f);
  private static final Color TRACK = new Color(0.09f, 0.09f, 0.11f, 0.8f);
  private static final Color TREAD = new Color(0.34f, 0.35f, 0.4f, 0.75f);

  /** One card riding the belt, with where it is now and the slot it is heading for. */
  private static final class Riding {
    private final SeedCard card;
    private final String plant;
    private float x;
    private int slot;
    private boolean wrapping;

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
    String ready = rule.peekReadyPlant();
    String key = ready == null ? "" : ready;
    if (!key.equals(shown)) {
      shown = key;
      advance();
    }
    paint();
  }

  private void load() {
    clearChildren();
    riding.clear();
    List<String> plants = rule.getBeltPlants();
    if (plants.isEmpty()) {
      addActor(empty);
      return;
    }
    for (int i = 0; i < slots(); i++) {
      String plant = plants.get(i % plants.size());
      SeedCard card = new SeedCard(skin, SeedCard.Size.COMPACT, plant, plant,
          plantArt.find(plant), hudArt, onPick);
      card.setSize(SeedBar.CARD_WIDTH, SeedBar.CARD_HEIGHT);
      addActor(card);
      riding.add(new Riding(card, plant, slotX(i), i));
    }
    shown = rule.peekReadyPlant() == null ? "" : rule.peekReadyPlant();
    orderReadyFirst();
    paint();
  }

  /**
   * Runs the belt on until the plant the rule is now offering has reached the head. Cards that
   * pass the near end doing so ride off it and are put back on at the far end, which is what keeps
   * the queue full; the rest simply close up the gap.
   */
  private void advance() {
    int shift = 1;
    String ready = rule.peekReadyPlant();
    if (ready != null) {
      for (Riding rider : riding) {
        if (rider.plant.equalsIgnoreCase(ready)) {
          shift = rider.slot;
          break;
        }
      }
    }
    if (shift <= 0 || riding.isEmpty()) {
      return;
    }
    for (Riding rider : riding) {
      int next = Math.floorMod(rider.slot - shift, riding.size());
      rider.wrapping |= next > rider.slot;
      rider.slot = next;
    }
  }

  /** Puts the plant the rule is offering at the head of the queue when the belt is first built. */
  private void orderReadyFirst() {
    String ready = rule.peekReadyPlant();
    if (ready == null) {
      return;
    }
    int head = -1;
    for (Riding rider : riding) {
      if (rider.plant.equalsIgnoreCase(ready)) {
        head = rider.slot;
        break;
      }
    }
    if (head <= 0) {
      return;
    }
    for (Riding rider : riding) {
      rider.slot = Math.floorMod(rider.slot - head, riding.size());
      rider.x = slotX(rider.slot);
    }
  }

  private void paint() {
    for (Riding rider : riding) {
      boolean available = rider.plant.equalsIgnoreCase(shown);
      rider.card.setStatus(available ? "free" : "queued");
      rider.card.setSelected(available);
      rider.card.setEnabled(available);
      rider.card.setTint(available ? READY : WAITING);
    }
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    float travel = delta * SLOTS_PER_SECOND * step();
    treadOffset = (treadOffset + travel) % TREAD_SPACING;
    for (Riding rider : riding) {
      float target = rider.wrapping ? offTrackLeft() : slotX(rider.slot);
      rider.x = Math.max(target, rider.x - travel);
      if (rider.wrapping && rider.x <= target) {
        rider.wrapping = false;
        // Back on at the far end, a whole belt ahead of where it is going, so several cards
        // wrapping at once come back on in the order they left.
        rider.x = slotX(rider.slot) + step() * riding.size();
      }
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

  /** One card per plant on the belt, so everything the rule can offer is on screen. */
  private int slots() {
    return rule.getBeltPlants().size();
  }

  private float step() {
    return SeedBar.CARD_WIDTH + SLOT_GAP;
  }

  private float slotX(int slot) {
    return TRACK_PAD + slot * step();
  }

  private float offTrackLeft() {
    return -step();
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
