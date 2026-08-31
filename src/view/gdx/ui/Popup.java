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
import java.util.Arrays;
import java.util.List;


public final class Popup {

  private static final float ROW_BUTTON_WIDTH = 210f;
  private static final float STACKED_BUTTON_WIDTH = 300f;

  public record Choice(String label, String style, Runnable action) {}

  private Popup() {
  }

  public static void show(Stage stage, Skin skin, String title, Actor body) {
    show(stage, skin, title, body, null, null);
  }

  public static void show(Stage stage, Skin skin, String title, Actor body, Choice... choices) {
    List<Choice> list = choices == null ? List.of() : Arrays.asList(choices);
    show(stage, skin, title, body, list, true, STACKED_BUTTON_WIDTH);
  }

  /**
   *
   * @param confirmText confirm button label, or null for a close-only popup
   * @param onConfirm runs before the popup closes
   */
  public static void show(Stage stage, Skin skin, String title, Actor body,
                          String confirmText, Runnable onConfirm) {
    show(stage, skin, title, body, confirmText, onConfirm,
        confirmText == null ? "Close" : "Cancel", null);
  }

  public static void show(Stage stage, Skin skin, String title, Actor body,
                          String confirmText, Runnable onConfirm,
                          String declineText, Runnable onDecline) {
    List<Choice> choices = new ArrayList<>();
    if (confirmText != null) {
      choices.add(new Choice(confirmText, UiSkinProvider.BUTTON_GREEN, onConfirm));
    }
    if (declineText != null) {
      choices.add(new Choice(declineText, UiSkinProvider.BUTTON_BROWN, onDecline));
    }
    show(stage, skin, title, body, choices, false, ROW_BUTTON_WIDTH);
  }

  private static void show(Stage stage, Skin skin, String title, Actor body,
                           List<Choice> choices, boolean stacked, float buttonWidth) {
    if (stage == null || skin == null) {
      return;
    }
    Table dim = dim(skin);
    Table panel = panel(skin, title, body);

    Table actions = new Table();
    actions.defaults().pad(stacked ? 6f : 8f).width(buttonWidth).height(66f);
    for (Choice choice : choices) {
      TextButton button = new TextButton(choice.label(), skin, choice.style());
      ButtonFeel.apply(button);
      button.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
          dim.remove();
          if (choice.action() != null) {
            choice.action().run();
          }
        }
      });
      actions.add(button);
      if (stacked) {
        actions.row();
      }
    }
    panel.add(actions).padTop(18f).row();

    dim.add(panel);
    stage.addActor(dim);
  }

  private static Table dim(Skin skin) {
    Table dim = new Table();
    dim.setFillParent(true);
    dim.setBackground(skin.getDrawable(UiSkinProvider.MODAL_DIM));
    dim.setTouchable(Touchable.enabled);
    return dim;
  }

  private static Table panel(Skin skin, String title, Actor body) {
    Table panel = new Table();
    panel.setBackground(new LayeredDrawable(
        skin.getDrawable(UiSkinProvider.PANEL_BACKGROUND),
        skin.getDrawable(UiSkinProvider.DIALOG_BORDER)));
    panel.pad(34f);
    panel.defaults().pad(6f);
    panel.add(new Label(title, skin, UiSkinProvider.LABEL_BIG)).padBottom(16f).row();
    if (body != null) {
      panel.add(body).row();
    }
    return panel;
  }
}
