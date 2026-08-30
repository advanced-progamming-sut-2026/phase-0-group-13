package view.gdx.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;

/**
 * The lift a button gets under the pointer, and the dip when it is pressed.
 *
 * <p>The doc's polish list asks for button animations alongside the menu transitions
 * {@code MenuScreen.playEntrance} already plays. Kept as one helper so every screen's buttons feel
 * the same and no screen has to remember the numbers.
 *
 * <p>Scaling a Scene2D widget needs {@code setTransform(true)}, which only groups have, so this
 * takes the {@link Table} that {@code TextButton} already is. The origin is set on the way in
 * rather than at construction, because a button has no size until the layout has run. Hit testing
 * is unaffected: the cell keeps its own bounds, only the drawing is scaled.
 */
public final class ButtonFeel {

  private static final float HOVER = 1.06f;
  private static final float PRESS = 0.96f;
  private static final float REST = 1f;
  private static final float SECONDS = 0.09f;

  private ButtonFeel() {}

  public static void apply(Table button) {
    button.setTransform(true);
    button.addListener(
        new ClickListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor from) {
            if (pointer == -1) {
              centreOrigin(button);
              scale(button, HOVER);
            }
          }

          @Override
          public void exit(InputEvent event, float x, float y, int pointer, Actor to) {
            if (pointer == -1) {
              scale(button, REST);
            }
          }

          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int b) {
            centreOrigin(button);
            scale(button, PRESS);
            return false;
          }

          @Override
          public void touchUp(InputEvent event, float x, float y, int pointer, int b) {
            scale(button, isOver() ? HOVER : REST);
          }
        });
  }

  private static void centreOrigin(Table button) {
    button.setOrigin(button.getWidth() / 2f, button.getHeight() / 2f);
  }

  private static void scale(Table button, float target) {
    button.clearActions();
    button.addAction(scaleTo(target, target, SECONDS, Interpolation.pow2Out));
  }
}
