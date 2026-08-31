package view.gdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import java.util.ArrayList;
import java.util.List;
import view.gdx.audio.GameAudio;

/**
 * The chapter chooser: the four worlds side by side, the selected one large and centred.
 *
 * <p>Everything is placed from the group's own size, so the row keeps its shape at any window size
 * and in fullscreen.
 */
public final class WorldCarousel extends WidgetGroup {

  /** Card height as a share of the group's height, for the selected card. */
  private static final float CARD_HEIGHT = 0.86f;
  private static final float SIDE_SCALE = 0.70f;
  private static final float SIDE_ALPHA = 0.62f;
  /** Gap between neighbouring cards, as a share of a card width. */
  private static final float GAP = 0.30f;
  private static final float GLIDE = 9f;

  /** Selection index while it slides; whole numbers are a settled selection. */
  private float shown;
  private int selected;

  private final List<Card> cards = new ArrayList<>();

  public interface Listener {
    void onSelected(int stage);

    void onEntered(int stage);
  }

  public WorldCarousel(Skin skin, MapArt art, int stages, int startStage, State state,
      Listener listener) {
    this.selected = MathUtils.clamp(startStage - 1, 0, stages - 1);
    this.shown = selected;
    setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.childrenOnly);

    for (int i = 0; i < stages; i++) {
      final int stage = i + 1;
      final int index = i;
      Card card = new Card(skin, art, stage, state.locked(stage), state.summary(stage));
      card.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          GameAudio.getInstance().play(GameAudio.Sfx.CLICK);
          selected = index;
          listener.onSelected(stage);
          listener.onEntered(stage);
        }
      });
      cards.add(card);
      addActor(card);
    }
  }

  /** What the screen knows about a chapter. */
  public interface State {
    boolean locked(int stage);

    String summary(int stage);
  }

  public int selectedStage() {
    return selected + 1;
  }

  public void select(int stage) {
    selected = MathUtils.clamp(stage - 1, 0, cards.size() - 1);
  }

  /** Steps the selection one world along, stopping at either end. Gives back the new stage. */
  public int step(int direction) {
    selected = MathUtils.clamp(selected + direction, 0, cards.size() - 1);
    return selectedStage();
  }

  public boolean canStep(int direction) {
    int next = selected + direction;
    return next >= 0 && next < cards.size();
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    shown = MathUtils.lerp(shown, selected, Math.min(1f, delta * GLIDE));
    place();
  }

  @Override
  public void layout() {
    shown = selected;
    place();
  }

  private void place() {
    float width = getWidth();
    float height = getHeight();
    if (width <= 0f || height <= 0f) {
      return;
    }
    float cardHeight = height * CARD_HEIGHT;
    // One step is a card plus a real gap, so the row reads as a wide path with space between the
    // worlds rather than as four pictures pushed together. The selected one sits in the middle
    // and its neighbours show at the edges.
    float step = cardHeight * cards.get(0).aspect() * (1f + GAP);

    for (int i = 0; i < cards.size(); i++) {
      Card card = cards.get(i);
      float offset = i - shown;
      float nearness = Math.max(0f, 1f - Math.abs(offset));
      float scale = SIDE_SCALE + (1f - SIDE_SCALE) * Interpolation.pow2Out.apply(nearness);

      float cardWidth = cardHeight * card.aspect();
      card.setSize(cardWidth, cardHeight);
      card.setOrigin(cardWidth / 2f, cardHeight / 2f);
      card.setScale(scale);
      card.setPosition(width / 2f + offset * step - cardWidth / 2f,
          height / 2f - cardHeight / 2f);
      card.getColor().a = SIDE_ALPHA + (1f - SIDE_ALPHA) * nearness;
      card.setDimmed(nearness < 0.5f);
    }
    cards.get(MathUtils.clamp(Math.round(shown), 0, cards.size() - 1)).toFront();
  }

  @Override
  public float getPrefWidth() {
    return 0f;
  }

  @Override
  public float getPrefHeight() {
    return 0f;
  }

  /** One world: its island art, a padlock when it is shut, and how far through it the player is. */
  private static final class Card extends Table {

    private static final Color OPEN = new Color(1f, 1f, 1f, 1f);
    private static final Color SHUT = new Color(0.45f, 0.46f, 0.56f, 1f);

    private final Image island;
    private final Label caption;
    private final boolean locked;
    private final float aspect;

    Card(Skin skin, MapArt art, int stage, boolean locked, String summary) {
      this.locked = locked;
      TextureRegion region = art.world(stage);
      this.aspect = region == null ? 0.85f
          : region.getRegionWidth() / (float) region.getRegionHeight();

      island = region == null ? new Image() : new Image(region);
      island.setScaling(Scaling.fit);
      island.setColor(locked ? SHUT : OPEN);

      Stack stack = new Stack();
      stack.add(island);
      if (locked) {
        Table badge = new Table();
        badge.add(new Image(skin.getDrawable(UiSkinProvider.LOCK_ICON))).size(56f, 72f);
        stack.add(badge);
      }
      add(stack).grow().row();

      caption = new Label(summary, skin, UiSkinProvider.LABEL_MEDIUM_OUTLINE);
      caption.setAlignment(com.badlogic.gdx.utils.Align.center);
      add(caption).padTop(2f);
    }

    float aspect() {
      return aspect;
    }

    void setDimmed(boolean dimmed) {
      caption.getColor().a = dimmed ? 0.7f : 1f;
      if (!locked) {
        island.setColor(dimmed ? new Color(0.78f, 0.80f, 0.86f, 1f) : OPEN);
      }
    }
  }
}
