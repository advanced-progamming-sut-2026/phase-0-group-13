package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import java.util.ArrayList;
import java.util.List;


/**
 * Modal for confirmations and detail views, so a small interaction doesn't need its own screen.
 *
 * <p>The dimmed backdrop is a filled touchable table, which is also what stops clicks getting
 * through to the screen underneath.
 */
public final class Popup {

  private Popup() {
  }

  /** One button on the popup: its label, skin style (e.g. BUTTON_GREEN) and what it runs. */
  public record Action(String label, String style, Runnable onClick) {
  }

  /** Detail popup with just a close button. */
  public static void show(Stage stage, Skin skin, String title, Actor body) {
    show(stage, skin, title, body, null, null);
  }

  /**
   * @param confirmText confirm button label, or null for a close-only popup
   * @param onConfirm runs before the popup closes
   */
  public static void show(Stage stage, Skin skin, String title, Actor body,
                          String confirmText, Runnable onConfirm) {
    show(stage, skin, title, body, confirmText, onConfirm,
        confirmText == null ? "Close" : "Cancel", null);
  }

  /** Two-action version. A null declineText leaves the popup with only the confirm button. */
  public static void show(Stage stage, Skin skin, String title, Actor body,
                          String confirmText, Runnable onConfirm,
                          String declineText, Runnable onDecline) {
    List<Action> actions = new ArrayList<>();
    if (confirmText != null) {
      actions.add(new Action(confirmText, UiSkinProvider.BUTTON_GREEN, onConfirm));
    }
    if (declineText != null) {
      actions.add(new Action(declineText, UiSkinProvider.BUTTON_BROWN, onDecline));
    }
    show(stage, skin, title, body, actions);
  }

  /** Any number of buttons, e.g. the pause menu's Resume/Restart/Save &amp; Exit. */
  public static void show(Stage stage, Skin skin, String title, Actor body, List<Action> actions) {
    if (stage == null || skin == null) {
      return;
    }

    Table dim = new Table();
    dim.setFillParent(true);
    dim.setBackground(skin.getDrawable(UiSkinProvider.MODAL_DIM));
    dim.setTouchable(Touchable.enabled);

    Table panel = new Table();
    panel.setBackground(skin.getDrawable(UiSkinProvider.DIALOG_BORDER));
    panel.pad(34f);
    panel.defaults().pad(6f);

    panel.add(new Label(title, skin, UiSkinProvider.LABEL_BIG)).padBottom(16f).row();
    if (body != null) {
      panel.add(body).row();
    }

    Table actionsRow = new Table();
    actionsRow.defaults().pad(8f).width(210f).height(66f);
    if (actions != null) {
      for (Action action : actions) {
        TextButton button = new TextButton(action.label(), skin, action.style());
        button.addListener(new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            if (action.onClick() != null) {
              action.onClick().run();
            }
            dim.remove();
          }
        });
        actionsRow.add(button);
      }
    }
    panel.add(actionsRow).padTop(18f).row();

    dim.add(panel);
    stage.addActor(dim);
  }
}
