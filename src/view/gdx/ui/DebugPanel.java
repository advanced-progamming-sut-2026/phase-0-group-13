package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import controller.MainMenuSubControllers.GameMenuController;
import controller.MainMenuSubControllers.GameMenuSubControllers.GamePlayController;
import data.persistence.UserManager;
import java.util.function.Consumer;
import model.core.GameManager;
import view.gdx.core.GameSettings;


public final class DebugPanel extends Table {

  private static final int COINS_PER_CLICK = 100;
  private static final int DIAMONDS_PER_CLICK = 10;
  private static final int SUN_PER_CLICK = 100;

  private final GameMenuController cheats = new GameMenuController();
  private final GamePlayController matchCheats = new GamePlayController();
  private final Consumer<String> notifier;

  public static boolean isEnabled() {
    return GameSettings.isDebugMode();
  }

  /**
   *
   * @param notifier where the result message goes, normally the screen's toast
   */
  public DebugPanel(Skin skin, Consumer<String> notifier) {
    this(skin, notifier, null);
  }

  public DebugPanel(Skin skin, Consumer<String> notifier, GameManager match) {
    this.notifier = notifier;

    add(new Label("debug", skin, "secondary")).padRight(10f);
    add(button(skin, "+" + COINS_PER_CLICK + " Coins", COINS_PER_CLICK, "coin")).padRight(8f);
    add(button(skin, "+" + DIAMONDS_PER_CLICK + " Diamonds", DIAMONDS_PER_CLICK, "diamond"));
    if (match == null) {
      add(unlockButton(skin)).padLeft(8f);
      return;
    }
    add(matchButton(skin, "+" + SUN_PER_CLICK + " Sun",
        "cheat add -n " + SUN_PER_CLICK + " suns",
        () -> "Added " + SUN_PER_CLICK + " sun.")).padLeft(8f);
    add(matchButton(skin, "+1 Plant Food", "cheat add-plant-food",
        () -> match.getPlantFoodCount() >= model.game.GameState.MAX_PLANT_FOOD
            ? "error: you cannot hold more than " + model.game.GameState.MAX_PLANT_FOOD
              + " plant foods"
            : "1 plant food added.")).padLeft(8f);
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

  private TextButton matchButton(Skin skin, String text, String command,
      java.util.function.Supplier<String> message) {
    TextButton button = new TextButton(text, skin, UiSkinProvider.BUTTON_PURPLE);
    button.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            String result = message.get();
            matchCheats.handleinput(command);
            DebugPanel.this.notify(result);
          }
        });
    return button;
  }

  private TextButton unlockButton(Skin skin) {
    TextButton button = new TextButton("Unlock Chapters", skin, UiSkinProvider.BUTTON_PURPLE);
    button.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            boolean loggedIn = UserManager.getInstance().getCurrentUser() != null;
            cheats.handleinput("menu cheat unlock-chapters");
            DebugPanel.this.notify(loggedIn
                ? "All chapters and levels unlocked."
                : "error: no user logged in");
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
