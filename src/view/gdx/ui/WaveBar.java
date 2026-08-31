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

public final class WaveBar extends Table {

  private static final String FLAG_REGION = "image_ui_hud_ingame_progress_meter_flag_default";
  private static final String BAR_STYLE = "xp_green";
  private static final float BAR_WIDTH = 250f;
  private static final float BAR_HEIGHT = 26f;
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

    Table markers = new Table();
    for (int i = 0; i < this.totalWaves; i++) {
      Image flag = new Image(flagRegion(skin));
      flag.setScaling(Scaling.fit);
      flags.add(flag);
      markers.add(flag).width(BAR_WIDTH / this.totalWaves).height(FLAG_HEIGHT);
    }

    Stack meterStack = new Stack();
    meterStack.add(meter);
    Table flagLayer = new Table();
    flagLayer.add(markers).expand().bottom();
    meterStack.add(flagLayer);

    add(caption).left().padRight(10f);
    add(meterStack).width(BAR_WIDTH).height(BAR_HEIGHT);
    update(0);
  }

  private static ProgressBar.ProgressBarStyle gameMeterStyle(Skin skin) {
    if (!HudPlates.has(skin, HudPlates.METER) || !HudPlates.has(skin, HudPlates.METER_FILL)) {
      return skin.get(BAR_STYLE, ProgressBar.ProgressBarStyle.class);
    }
    ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle();
    style.background = HudPlates.drawable(skin, HudPlates.METER);
    style.background.setMinHeight(BAR_HEIGHT);
    style.background.setMinWidth(0f);
    style.knobBefore = HudPlates.drawable(skin, HudPlates.METER_FILL);
    style.knobBefore.setMinHeight(BAR_HEIGHT - 8f);
    style.knobBefore.setMinWidth(0f);
    return style;
  }

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
