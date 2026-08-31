package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/**
 * Penny and Crazy Dave's lines, for the two moments the spec asks for: arriving in a chapter and
 * beating its Zomboss.
 */
public final class Dialogue {

  public static final String PENNY = "Penny";
  public static final String CRAZY_DAVE = "Crazy Dave";

  private Dialogue() {
  }

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

  public static String zombossTitle(String seasonName) {
    switch (key(seasonName)) {
      case "egypt":
        return "Dr. Zomboss   -   Robot";
      case "frost":
        return "Dr. Zomboss   -   Mammoth";
      case "beach":
        return "Dr. Zomboss   -   Octopus";
      default:
        return "Dr. Zomboss   -   Dragon";
    }
  }

  public static String bossWarning(String seasonName) {
    switch (key(seasonName)) {
      case "egypt":
        return "His robot fires missiles and charges the lanes it stands in. Keep planting.";
      case "frost":
        return "The mammoth freezes whole rows and whole columns. Fire plants are your friend.";
      case "beach":
        return "He sends octopuses for your plants and sucks two rows into his mouth. Spread out.";
      default:
        return "The dragon burns rows to the ground and drops imps in the flames. Stay spread out.";
    }
  }

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
