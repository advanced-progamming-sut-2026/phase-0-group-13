package view.gdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import java.util.ArrayList;
import java.util.List;
import model.game.zombie.behavior.ZombossHealth;

public final class BossBar extends Table {

  private static final float SEGMENT_WIDTH = 76f;
  private static final float BAR_HEIGHT = 22f;
  private static final float SEGMENT_GAP = 4f;

  private static final Color FILL = new Color(1f, 0.42f, 0.34f, 1f);
  private static final Color CALM = new Color(1f, 1f, 1f, 1f);
  private static final Color STUNNED = new Color(1f, 0.88f, 0.35f, 1f);

  private final List<ProgressBar> segments = new ArrayList<>();
  private final Label caption;
  private final String bossName;

  private int shownHealth = Integer.MIN_VALUE;
  private boolean shownStun;

  public BossBar(Skin skin, String bossName) {
    this.bossName = bossName == null || bossName.isBlank() ? "Dr. Zomboss" : bossName;
    caption = new Label(this.bossName, skin, UiSkinProvider.LABEL_MEDIUM_OUTLINE);
    add(caption).left().padRight(10f);

    for (int i = 0; i < ZombossHealth.SEGMENTS; i++) {
      ProgressBar segment = new ProgressBar(0f, 1f, 0.01f, false, segmentStyle(skin));
      segment.setAnimateDuration(0.15f);
      segment.setValue(1f);
      segment.setColor(CALM);
      segments.add(segment);
    }

    Table meters = new Table();
    for (int i = segments.size() - 1; i >= 0; i--) {
      meters.add(segments.get(i)).width(SEGMENT_WIDTH).height(BAR_HEIGHT)
          .padLeft(i == segments.size() - 1 ? 0f : SEGMENT_GAP);
    }
    add(meters);
  }

  private static ProgressBar.ProgressBarStyle segmentStyle(Skin skin) {
    Drawable trough = HudPlates.drawable(skin, HudPlates.METER);
    Drawable fill = HudPlates.drawable(skin, HudPlates.METER_FILL);
    if (trough == null || !(fill instanceof TextureRegionDrawable pill)) {
      return skin.get("xp_green", ProgressBar.ProgressBarStyle.class);
    }
    Drawable red = pill.tint(FILL);
    trough.setMinHeight(BAR_HEIGHT);
    trough.setMinWidth(0f);
    red.setMinHeight(BAR_HEIGHT - 8f);
    red.setMinWidth(0f);

    ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle();
    style.background = trough;
    style.knobBefore = red;
    return style;
  }

  /**
   *
   * @param health the boss's segment split
   * @param currentHealth what it has left
   * @param stunned whether it is in the opening between two segments
   */
  public void update(ZombossHealth health, int currentHealth, boolean stunned) {
    if (health == null || (currentHealth == shownHealth && stunned == shownStun)) {
      return;
    }
    shownHealth = currentHealth;
    shownStun = stunned;
    for (int i = 0; i < segments.size(); i++) {
      segments.get(i).setValue(health.fractionOf(i, currentHealth));
      segments.get(i).setColor(stunned ? STUNNED : CALM);
    }
    caption.setText(stunned ? bossName + "   STUNNED" : bossName);
  }
}
