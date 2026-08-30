package view.gdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import java.util.ArrayList;
import java.util.List;

/**
 * How far through the waves the match is: a meter plus one flag per wave.
 *
 * <p>Counts nothing itself. GameplayScreen feeds it GameManager's own wave index, so the meter
 * cannot disagree with the match.
 */
public final class WaveBar extends Table {

  private static final String FLAG_REGION = "image_ui_hud_ingame_progress_meter_flag_default";
  private static final String BAR_STYLE = "xp_green";
  private static final float BAR_WIDTH = 250f;
  /** Tall enough that the meter reads across the room; the art is 33px in its own page. */
  private static final float BAR_HEIGHT = 26f;
  /** Flags ride on the meter, so they are sized against it rather than given a row of their own. */
  private static final float FLAG_HEIGHT = 16f;

  private static final Color CLEARED = new Color(1f, 0.85f, 0.3f, 1f);
  private static final Color CURRENT = new Color(1f, 1f, 1f, 1f);
  private static final Color PENDING = new Color(1f, 1f, 1f, 0.3f);

  private final List<Image> flags = new ArrayList<>();
  private final ProgressBar meter;
  private final Label caption;
  private final int totalWaves;

  private int shownWave = -1;

  public WaveBar(Skin skin, int totalWaves) {
    this.totalWaves = Math.max(1, totalWaves);
    caption = new Label("", skin, UiSkinProvider.LABEL_MEDIUM_OUTLINE);
    meter = new ProgressBar(0f, this.totalWaves, 1f, false, gameMeterStyle(skin));
    meter.setAnimateDuration(0.3f);

    // One marker per wave, sharing the meter's width so a flag sits where that wave lands.
    Table markers = new Table();
    for (int i = 0; i < this.totalWaves; i++) {
      Image flag = new Image(flagRegion(skin));
      flag.setScaling(Scaling.fit);
      flags.add(flag);
      markers.add(flag).width(BAR_WIDTH / this.totalWaves).height(FLAG_HEIGHT);
    }

    // Caption beside the meter and flags on top of it, rather than a three-row stack.
    //
    // Height is the scarce dimension up here: everything the HUD puts above the seed bar comes
    // off the top of the lawn, and a 60px tall wave meter was pushing the seed cards down onto
    // the board's first lane. Flags standing on the meter is also how the original draws it.
    Stack meterStack = new Stack();
    meterStack.add(meter);
    Table flagLayer = new Table();
    flagLayer.add(markers).expand().bottom();
    meterStack.add(flagLayer);

    add(caption).left().padRight(10f);
    add(meterStack).width(BAR_WIDTH).height(BAR_HEIGHT);
    update(0);
  }

  /**
   * The original game's own wave meter, if the skin has it.
   *
   * <p>The generic {@code xp_green} bar this used to borrow is a hairline on a grey ground: on a
   * sand or an ice backdrop it disappears, which for the one readout that says how much of the
   * level is left is the wrong outcome. The skin carries PvZ2's in-game meter -- a dark trough and
   * a green pill -- and nothing was drawing it. Falls back to the old style when it is absent.
   */
  private static ProgressBar.ProgressBarStyle gameMeterStyle(Skin skin) {
    if (!HudPlates.has(skin, HudPlates.METER) || !HudPlates.has(skin, HudPlates.METER_FILL)) {
      return skin.get(BAR_STYLE, ProgressBar.ProgressBarStyle.class);
    }
    ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle();
    style.background = HudPlates.drawable(skin, HudPlates.METER);
    style.background.setMinHeight(BAR_HEIGHT);
    style.background.setMinWidth(0f);
    // knobBefore with no knob is how Scene2D draws a plain filled meter: the fill is stretched
    // from the left edge to the value and there is no slider head to go with it.
    style.knobBefore = HudPlates.drawable(skin, HudPlates.METER_FILL);
    style.knobBefore.setMinHeight(BAR_HEIGHT - 8f);
    style.knobBefore.setMinWidth(0f);
    return style;
  }

  /** The skin's flag, or the white pixel when the atlas has no flag art. */
  private static TextureRegion flagRegion(Skin skin) {
    return skin.has(FLAG_REGION, TextureRegion.class)
        ? skin.getRegion(FLAG_REGION) : skin.getRegion("white-pixel");
  }

  /** @param currentWaveIndex GameManager.getCurrentWaveIndex(), 0-based */
  public void update(int currentWaveIndex) {
    int wave = Math.max(0, Math.min(currentWaveIndex, totalWaves));
    if (wave == shownWave) {
      return;
    }
    shownWave = wave;
    meter.setValue(wave);
    caption.setText("wave " + Math.min(wave + 1, totalWaves) + " / " + totalWaves);
    for (int i = 0; i < flags.size(); i++) {
      flags.get(i).setColor(i < wave ? CLEARED : i == wave ? CURRENT : PENDING);
    }
  }
}
