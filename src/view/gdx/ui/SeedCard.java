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
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import java.util.function.Consumer;

public final class SeedCard extends Table {

  public enum Size {
    COMPACT,
    FULL
  }

  private static final Color NORMAL = new Color(1f, 1f, 1f, 1f);
  private static final Color DISABLED = new Color(1f, 1f, 1f, 0.72f);
  private static final Color BOOST_COLOR = new Color(1f, 0.82f, 0.25f, 1f);
  private static final Color PICKED_NAME = new Color(1f, 0.88f, 0.35f, 1f);

  private final Skin skin;
  private final String key;
  private final HudArt hudArt;
  private final boolean compact;

  private final Label nameLabel;
  private final Label statusLabel;
  private final Label boostLabel;
  private final Table costRow;

  private boolean selected;
  private boolean enabled = true;
  private String cardArt;

  private final float lockedPrefWidth;
  private final float lockedPrefHeight;

  public SeedCard(Skin skin, Size size, String key, String label, TextureRegion art,
      HudArt hudArt, Consumer<String> onSelect) {
    this.skin = skin;
    this.key = key;
    this.hudArt = hudArt;

    this.compact = size == Size.COMPACT;
    float artWidth = compact ? 52f : 92f;
    float artHeight = compact ? 40f : 62f;
    float textWidth = compact ? 96f : 118f;

    setBackground(skin.getDrawable(UiSkinProvider.PANEL_BACKGROUND));
    if (compact) {
      pad(4f);
    } else {
      pad(17f, 22f, 24f, 22f);
    }

    if (art != null) {
      Image portrait = new Image(art);
      portrait.setScaling(Scaling.fit);
      add(portrait).size(artWidth, artHeight).colspan(2).padBottom(2f).row();
    } else {
      Label none = new Label("no art", skin, "secondary");
      none.setAlignment(Align.center);
      add(none).size(artWidth, artHeight).colspan(2).padBottom(2f).row();
    }

    nameLabel = new Label(label, skin, compact ? "secondary" : UiSkinProvider.LABEL_MEDIUM);
    nameLabel.setWrap(true);
    nameLabel.setAlignment(Align.center);
    if (compact) {
      nameLabel.setFontScale(UiSkinProvider.fontScale(0.8f));
    }
    add(nameLabel).width(textWidth).colspan(2).row();

    boostLabel = new Label("", skin, "secondary");
    boostLabel.setColor(BOOST_COLOR);
    costRow = new Table();
    Table priceLine = new Table();
    priceLine.add(costRow);
    priceLine.add(boostLabel).padLeft(6f);
    add(priceLine).colspan(2).row();

    statusLabel = new Label("", skin, "secondary");
    statusLabel.setAlignment(Align.center);
    add(statusLabel).width(textWidth).colspan(2);

    if (onSelect != null) {
      setTouchable(Touchable.enabled);
      addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          if (enabled) {
            onSelect.accept(key);
          }
        }
      });
    }

    // A full card gets room for the frame; a compact one never wears it, so content size is enough.
    float border = this.compact ? 0f : 1f;
    lockedPrefWidth = Math.max(super.getPrefWidth(),
        border * skin.getDrawable(UiSkinProvider.DIALOG_BORDER).getMinWidth());
    lockedPrefHeight = Math.max(super.getPrefHeight(),
        border * skin.getDrawable(UiSkinProvider.DIALOG_BORDER).getMinHeight());
  }

  @Override
  public float getPrefWidth() {
    return lockedPrefWidth;
  }

  @Override
  public float getPrefHeight() {
    return lockedPrefHeight;
  }

  @Override
  public float getMinWidth() {
    return lockedPrefWidth;
  }

  @Override
  public float getMinHeight() {
    return lockedPrefHeight;
  }

  public String getKey() {
    return key;
  }

  public SeedCard withCost(int sunCost) {
    costRow.clear();
    TextureRegion sun = hudArt == null ? null : hudArt.find("sun");
    if (sun != null) {
      Image icon = new Image(sun);
      icon.setScaling(Scaling.fit);
      costRow.add(icon).size(18f, 18f).padRight(4f);
    }
    costRow.add(new Label(String.valueOf(sunCost), skin, UiSkinProvider.LABEL_MEDIUM));
    return this;
  }

  public void setStatus(String text) {
    statusLabel.setText(text == null ? "" : text);
  }

  public void setBoosted(boolean boosted) {
    boostLabel.setText(boosted ? "BOOST" : "");
  }

  public SeedCard withLock() {
    if (compact) {
      return this;
    }
    Image lock = new Image(skin.getDrawable(UiSkinProvider.LOCK_ICON));
    Table corner = new Table();
    corner.top().right().pad(7f);
    corner.setFillParent(true);
    corner.add(lock).size(22f, 29f);
    addActor(corner);
    return this;
  }

  public SeedCard withCardArt(String drawableName) {
    if (compact || drawableName == null) {
      return this;
    }
    this.cardArt = drawableName;
    applyBackground();
    return this;
  }

  public void setSelected(boolean selected) {
    if (this.selected == selected) {
      return;
    }
    this.selected = selected;
    if (compact) {
      nameLabel.setColor(selected ? PICKED_NAME : Color.WHITE);
      return;
    }
    applyBackground();
  }

  private void applyBackground() {
    Drawable resting = skin.getDrawable(
        cardArt == null ? UiSkinProvider.PANEL_BACKGROUND : cardArt);
    setBackground(selected
        ? new LayeredDrawable(resting, skin.getDrawable(UiSkinProvider.DIALOG_BORDER))
        : resting);
  }

  public boolean isSelected() {
    return selected;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    setColor(enabled ? NORMAL : DISABLED);
  }

  public void setTint(Color tint) {
    setColor(tint);
  }
}
