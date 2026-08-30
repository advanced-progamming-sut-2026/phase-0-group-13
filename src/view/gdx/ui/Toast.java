package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.SnapshotArray;


/**
 * The temporary message every menu uses to report an error or a success.
 *
 * <p>The messages themselves come from the Phase 1 logic (the text of the exception UserManager
 * throws, mostly), so this only decides how long they stay up. It floats above the layout in a
 * fill-parent table instead of sitting in it, so a long message can't push the form around.
 *
 * <p>The skin has no Window style, so there is nothing modal to show here and nothing to dismiss.
 */
public final class Toast {

  private static final float HOLD_SECONDS = 2.5f;
  private static final float FADE_SECONDS = 0.4f;
  /**
   * How far down the message lands.
   *
   * <p>Clear of the in-match HUD block: at 84 it was landing across the seed cards and the tool
   * buttons, so a refusal and the row of controls it was about were drawn over each other. Below
   * them it crosses the top of the lawn for a couple of seconds instead, which is where the
   * original puts its own banners.
   */
  private static final float TOP_PADDING = 205f;
  private static final float MAX_WIDTH = 640f;
  /** Past this many characters the message wraps instead of stretching the plate. */
  private static final int WRAP_AFTER_CHARACTERS = 58;

  private Toast() {
  }

  private static final com.badlogic.gdx.graphics.Color ALERT =
      new com.badlogic.gdx.graphics.Color(1f, 0.28f, 0.24f, 1f);

  /** Drops a message onto the stage that fades out and removes itself. */
  public static void show(Stage stage, Skin skin, String message) {
    show(stage, skin, message, null);
  }

  /**
   * Same message, further down the screen.
   *
   * <p>For a layout whose top strip is already full: the arcade mini-games put their picker where
   * the default band is, and a refusal drawn behind a row of cards is a refusal nobody reads.
   */
  public static void show(Stage stage, Skin skin, String message, float topPadding) {
    show(stage, skin, message, null, topPadding);
  }

  /** The red in-match warning: a new wave, necromancy, the tide turning. */
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
    // Only a long message wraps. A wrapped label has no preferred width of its own, so the cell
    // has to be told one, and that made the plate the same wide bar for "saved." as for a full
    // sentence. Short messages keep their natural width and the plate hugs them.
    boolean wraps = message.length() > WRAP_AFTER_CHARACTERS;
    label.setWrap(wraps);
    label.setAlignment(Align.center);
    if (colour != null) {
      label.setColor(colour);
    }

    // A plate under the type, because in a match this lands on the world art: sand, ice or a
    // night sky, none of which a thin outlined line of text survives. The plate is the original
    // game's own HUD backing, so it belongs to the same set as the counters and the tools.
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

  /** Anything still on screen from the previous click would overlap the new message. */
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
