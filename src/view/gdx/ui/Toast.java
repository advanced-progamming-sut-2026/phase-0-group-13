package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.SnapshotArray;


public final class Toast {

  private static final float HOLD_SECONDS = 2.5f;
  private static final float FADE_SECONDS = 0.4f;
  private static final float TOP_PADDING = 205f;
  private static final float MAX_WIDTH = 640f;
  private static final int WRAP_AFTER_CHARACTERS = 58;

  private Toast() {
  }

  private static final com.badlogic.gdx.graphics.Color ALERT =
      new com.badlogic.gdx.graphics.Color(1f, 0.28f, 0.24f, 1f);

  public static void show(Stage stage, Skin skin, String message) {
    show(stage, skin, message, null);
  }

  public static void show(Stage stage, Skin skin, String message, float topPadding) {
    show(stage, skin, message, null, topPadding);
  }

  public static void showAlert(Stage stage, Skin skin, String message) {
    show(stage, skin, message, ALERT);
  }

  private static void show(
      Stage stage, Skin skin, String message, com.badlogic.gdx.graphics.Color colour) {
    show(stage, skin, message, colour, TOP_PADDING);
  }

  private static void show(Stage stage, Skin skin, String message,
      com.badlogic.gdx.graphics.Color colour, float topPadding) {
    if (stage == null || skin == null || message == null || message.isEmpty()) {
      return;
    }
    removePrevious(stage);

    Label label = new Label(message, skin, UiSkinProvider.LABEL_MEDIUM);
    boolean wraps = message.length() > WRAP_AFTER_CHARACTERS;
    label.setWrap(wraps);
    label.setAlignment(Align.center);
    if (colour != null) {
      label.setColor(colour);
    }

    Table plate = new Table();
    com.badlogic.gdx.scenes.scene2d.utils.Drawable backing = HudPlates.plate(skin);
    if (backing != null) {
      plate.setBackground(backing);
    }
    plate.pad(8f, 22f, 10f, 22f);
    if (wraps) {
      plate.add(label).width(MAX_WIDTH);
    } else {
      plate.add(label);
    }

    Table holder = new Table();
    holder.setFillParent(true);
    holder.top().padTop(topPadding);
    holder.add(plate);
    holder.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
    holder.setUserObject(Toast.class);

    holder.getColor().a = 0f;
    holder.addAction(
        Actions.sequence(
            Actions.fadeIn(FADE_SECONDS),
            Actions.delay(HOLD_SECONDS),
            Actions.fadeOut(FADE_SECONDS),
            Actions.removeActor()));

    stage.addActor(holder);
  }

  private static void removePrevious(Stage stage) {
    SnapshotArray<Actor> actors = stage.getRoot().getChildren();
    Actor[] snapshot = actors.begin();
    for (int i = 0, n = actors.size; i < n; i++) {
      if (snapshot[i].getUserObject() == Toast.class) {
        snapshot[i].remove();
      }
    }
    actors.end();
  }
}
