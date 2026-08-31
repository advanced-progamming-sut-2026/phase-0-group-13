package view.gdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import java.util.ArrayList;
import java.util.List;
import view.gdx.audio.GameAudio;

public final class LevelMap extends WidgetGroup {

  private static final float[][] ROUTE = {
      {0.295f, 0.40f},
      {0.485f, 0.63f},
      {0.675f, 0.36f},
      {0.875f, 0.57f},
  };

  private static final float HUB_X = 0.105f;
  private static final float HUB_Y = 0.54f;
  private static final float HUB_HEIGHT = 0.50f;
  private static final float NODE_HEIGHT = 0.21f;
  private static final float LINK_THICKNESS = 0.030f;
  private static final float PORTAL_HEIGHT = 0.34f;

  private static final Color LINK_OPEN = new Color(1f, 1f, 1f, 0.95f);
  private static final Color LINK_SHUT = new Color(0.62f, 0.66f, 0.76f, 0.40f);

  public enum NodeState { LOCKED, AVAILABLE, COMPLETED, BOSS }

  private final MapArt art;
  private final int stage;
  private final TextureRegion hub;
  private final TextureRegion link;
  private final TextureRegion portal;
  private final TextureRegion spark;
  private final TextureRegion decor1;
  private final TextureRegion decor2;

  private final List<Node> nodes = new ArrayList<>();
  private int selected;
  private float pulse;

  public LevelMap(Skin skin, MapArt art, int stage, int levels, Source source, Listener listener) {
    this.art = art;
    this.stage = stage;
    this.hub = art.piece(stage, "hub");
    this.link = art.piece(stage, "link");
    this.portal = art.piece(stage, "portal");
    this.spark = art.piece(stage, "spark");
    this.decor1 = art.piece(stage, "decor1");
    this.decor2 = art.piece(stage, "decor2");
    setTouchable(Touchable.childrenOnly);

    for (int i = 0; i < levels; i++) {
      final int level = i + 1;
      Node node = new Node(skin, level, source.stateOf(level));
      if (node.state != NodeState.LOCKED) {
        node.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            GameAudio.getInstance().play(GameAudio.Sfx.CLICK);
            selected = level - 1;
            listener.onLevelChosen(level);
          }
        });
      } else {
        node.setTouchable(Touchable.disabled);
      }
      nodes.add(node);
      addActor(node);
    }
    selected = MathUtils.clamp(source.currentLevel() - 1, 0, Math.max(0, levels - 1));
  }

  public interface Source {
    NodeState stateOf(int level);

    int currentLevel();
  }

  public interface Listener {
    void onLevelChosen(int level);
  }

  public int selectedLevel() {
    return selected + 1;
  }

  public void select(int level) {
    selected = MathUtils.clamp(level - 1, 0, nodes.size() - 1);
  }

  public int step(int direction) {
    selected = MathUtils.clamp(selected + direction, 0, nodes.size() - 1);
    return selectedLevel();
  }

  public boolean canStep(int direction) {
    int next = selected + direction;
    return next >= 0 && next < nodes.size();
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    pulse += delta;
    place();
  }

  @Override
  public void layout() {
    place();
  }

  private void place() {
    float width = getWidth();
    float height = getHeight();
    if (width <= 0f || height <= 0f) {
      return;
    }
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      float[] at = ROUTE[i % ROUTE.length];
      float nodeHeight = height * NODE_HEIGHT;
      float nodeWidth = nodeHeight * node.aspect();
      float grow = i == selected ? 1.12f + 0.03f * MathUtils.sin(pulse * 3f) : 1f;
      node.setSize(nodeWidth, nodeHeight);
      node.setOrigin(nodeWidth / 2f, nodeHeight / 2f);
      node.setScale(grow);
      node.setPosition(width * at[0] - nodeWidth / 2f, height * at[1] - nodeHeight / 2f);
    }
  }

  private float nodeCentreX(int index) {
    return getX() + getWidth() * ROUTE[index % ROUTE.length][0];
  }

  private float nodeCentreY(int index) {
    return getY() + getHeight() * ROUTE[index % ROUTE.length][1];
  }

  @Override
  public void draw(Batch batch, float parentAlpha) {
    validate();
    float width = getWidth();
    float height = getHeight();
    Color tint = getColor();
    float alpha = tint.a * parentAlpha;

    batch.setColor(1f, 1f, 1f, alpha * 0.85f);
    drawPiece(batch, decor1, 0.545f, 0.145f, height * 0.16f);
    drawPiece(batch, decor2, 0.735f, 0.855f, height * 0.15f);

    batch.setColor(1f, 1f, 1f, alpha);
    if (hub != null) {
      float h = height * HUB_HEIGHT;
      float w = h * hub.getRegionWidth() / (float) hub.getRegionHeight();
      batch.draw(hub, getX() + width * HUB_X - w / 2f, getY() + height * HUB_Y - h / 2f, w, h);
    }

    drawLinks(batch, alpha);
    drawHere(batch, alpha);

    batch.setColor(1f, 1f, 1f, alpha * (0.55f + 0.35f * MathUtils.sin(pulse * 2.2f)));
    drawPiece(batch, spark, 0.395f, 0.775f, height * 0.075f);
    drawPiece(batch, spark, 0.712f, 0.235f, height * 0.06f);

    batch.setColor(1f, 1f, 1f, alpha);
    super.draw(batch, parentAlpha);
  }

  private void drawPiece(Batch batch, TextureRegion region, float fx, float fy, float targetHeight) {
    if (region == null) {
      return;
    }
    float w = targetHeight * region.getRegionWidth() / (float) region.getRegionHeight();
    batch.draw(region, getX() + getWidth() * fx - w / 2f,
        getY() + getHeight() * fy - targetHeight / 2f, w, targetHeight);
  }

  private void drawLinks(Batch batch, float alpha) {
    if (link == null) {
      return;
    }
    float thickness = getHeight() * LINK_THICKNESS;
    for (int i = 0; i < nodes.size() - 1; i++) {
      float x1 = nodeCentreX(i);
      float y1 = nodeCentreY(i);
      float x2 = nodeCentreX(i + 1);
      float y2 = nodeCentreY(i + 1);
      boolean travelled = nodes.get(i).state != NodeState.LOCKED
          && nodes.get(i + 1).state != NodeState.LOCKED;
      Color colour = travelled ? LINK_OPEN : LINK_SHUT;
      batch.setColor(colour.r, colour.g, colour.b, colour.a * alpha);

      float dx = x2 - x1;
      float dy = y2 - y1;
      float span = (float) Math.sqrt(dx * dx + dy * dy);
      batch.draw(link, x1, y1 - thickness / 2f, 0f, thickness / 2f, span, thickness,
          1f, 1f, MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees);
    }
    batch.setColor(1f, 1f, 1f, alpha);
  }

  private void drawHere(Batch batch, float alpha) {
    if (portal == null || selected < 0 || selected >= nodes.size()) {
      return;
    }
    if (nodes.get(selected).state == NodeState.LOCKED) {
      return;
    }
    float h = getHeight() * PORTAL_HEIGHT * (1f + 0.05f * MathUtils.sin(pulse * 2.4f));
    float w = h * portal.getRegionWidth() / (float) portal.getRegionHeight();
    batch.setColor(1f, 1f, 1f, alpha * 0.8f);
    batch.draw(portal, nodeCentreX(selected) - w / 2f, nodeCentreY(selected) - h / 2f, w, h);
    batch.setColor(1f, 1f, 1f, alpha);
  }

  @Override
  public float getPrefWidth() {
    return 0f;
  }

  @Override
  public float getPrefHeight() {
    return 0f;
  }

  private final class Node extends Group {

    private final TextureRegion region;
    private final Label number;
    private final NodeState state;

    Node(Skin skin, int level, NodeState state) {
      this.state = state;
      this.region = art.piece(stage, regionFor(state));
      // Transformed, so scaling the node scales its number with it and the two never come apart.
      setTransform(true);
      number = new Label(String.valueOf(level), skin, UiSkinProvider.LABEL_BIG_OUTLINE);
      number.setAlignment(Align.center);
      number.setTouchable(Touchable.disabled);
      if (state == NodeState.LOCKED) {
        number.getColor().a = 0.55f;
      }
      addActor(number);
    }

    float aspect() {
      return region == null ? 1.1f
          : region.getRegionWidth() / (float) region.getRegionHeight();
    }

    private String regionFor(NodeState state) {
      return switch (state) {
        case COMPLETED -> "node_done";
        case LOCKED -> "node_locked";
        case BOSS -> "node_boss";
        default -> "node_open";
      };
    }

    @Override
    protected void drawChildren(Batch batch, float parentAlpha) {
      number.setSize(getWidth(), getHeight() * 0.34f);
      number.setPosition(0f, getHeight() * 0.45f);
      super.drawChildren(batch, parentAlpha);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
      if (region != null) {
        applyTransform(batch, computeTransform());
        batch.setColor(1f, 1f, 1f, getColor().a * parentAlpha);
        batch.draw(region, 0f, 0f, getWidth(), getHeight());
        resetTransform(batch);
      }
      super.draw(batch, parentAlpha);
    }
  }
}
