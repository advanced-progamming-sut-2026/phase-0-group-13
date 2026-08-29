package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/**
 * Penny and Crazy Dave's lines, for the two moments the spec asks for: arriving in a chapter and
 * beating its Zomboss.
 *
 * <p>Text and a popup, nothing else -- the screens decide when a line is due.
 */
public final class Dialogue {

  public static final String PENNY = "Penny";
  public static final String CRAZY_DAVE = "Crazy Dave";

  private Dialogue() {
  }

  /** Penny's briefing for a season. She addresses whoever is signed in, not a stand-in name. */
  public static String stageStart(String seasonName, String playerName) {
    String player = playerName == null || playerName.isBlank() ? "Commander" : playerName.trim();
    switch (key(seasonName)) {
      case "egypt":
        return "Ancient Egypt, " + player + ". Mind the tombstones -- nothing grows on them.";
      case "frost":
        return "Frostbite Caves. The ice will freeze your plants solid, so keep the lanes clear.";
      case "beach":
        return "Big Wave Beach. The tide comes in with every wave; only water plants survive it.";
      default:
        return "The Dark Ages. No sun falls at night, " + player + " -- grow your own.";
    }
  }

  /** What Crazy Dave says once the season's Zomboss is down. */
  public static String afterZomboss(String seasonName) {
    switch (key(seasonName)) {
      case "egypt":
        return "WABBY WABBO! That mummy machine never stood a chance!";
      case "frost":
        return "Chilly! I'm gonna eat my taco now. It's been in the ice for 3000 years!";
      case "beach":
        return "You beat him! I'm gonna go surf. Wait, I can't swim. CRAZY!";
      default:
        return "The Dark Ages are over! Somebody get me a torch, I can't see my taco!";
    }
  }

  private static String key(String seasonName) {
    String name = seasonName == null ? "" : seasonName.toLowerCase();
    if (name.contains("egypt")) {
      return "egypt";
    }
    if (name.contains("frost") || name.contains("cave")) {
      return "frost";
    }
    return name.contains("beach") || name.contains("wave") ? "beach" : "dark";
  }

  /** One speaker, one line, one button. */
  public static void show(Stage stage, Skin skin, String speaker, String line, Runnable onClose) {
    if (stage == null || skin == null || line == null) {
      return;
    }
    Label text = new Label(line, skin, UiSkinProvider.LABEL_MEDIUM);
    text.setWrap(true);
    Table body = new Table();
    body.add(text).width(440f);
    Popup.show(stage, skin, speaker, body,
        new Popup.Choice("Got it", UiSkinProvider.BUTTON_GREEN, onClose));
  }
}
