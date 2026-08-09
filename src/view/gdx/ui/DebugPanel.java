package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import controller.MainMenuSubControllers.GameMenuController;
import data.persistence.UserManager;
import java.util.function.Consumer;
import view.gdx.core.GdxConfig;


/**
 * The debug-only currency cheats.
 *
 * <p>The buttons don't add anything themselves, they hand the Phase 1 cheat command to the Phase 1
 * GameMenuController, so there is exactly one cheat implementation and the graphical one can't
 * drift from the typed one. It doesn't save either, same as the terminal cheat.
 *
 * <p>Only built when {@link GdxConfig#DEBUG_MODE} is on. Callers use {@link #isEnabled()} rather
 * than reading the flag themselves, so the check lives in one place.
 */
public final class DebugPanel extends Table {

  private static final int COINS_PER_CLICK = 100;
  private static final int DIAMONDS_PER_CLICK = 10;

  private final GameMenuController cheats = new GameMenuController();
  private final Consumer<String> notifier;

  /** Whether the debug controls should be built at all. */
  public static boolean isEnabled() {
    return GdxConfig.DEBUG_MODE;
  }

  /**
   * @param notifier where the result message goes, normally the screen's toast
   */
  public DebugPanel(Skin skin, Consumer<String> notifier) {
    this.notifier = notifier;

    add(new Label("debug", skin, "secondary")).padRight(10f);
    add(button(skin, "+" + COINS_PER_CLICK + " Coins", COINS_PER_CLICK, "coin")).padRight(8f);
    add(button(skin, "+" + DIAMONDS_PER_CLICK + " Diamonds", DIAMONDS_PER_CLICK, "diamond"));
  }

  private TextButton button(Skin skin, String text, int count, String currency) {
    TextButton button = new TextButton(text, skin, UiSkinProvider.BUTTON_PURPLE);
    button.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            runCheat(count, currency);
          }
        });
    return button;
  }

  private void runCheat(int count, String currency) {
    if (UserManager.getInstance().getCurrentUser() == null) {
      notify("error: no user logged in");
      return;
    }
    cheats.handleinput("menu cheat add " + count + " " + currency);
    notify("Added " + count + " " + currency + "(s).");
  }

  private void notify(String message) {
    if (notifier != null) {
      notifier.accept(message);
    }
  }
}
