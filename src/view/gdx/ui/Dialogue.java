package view.gdx.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import java.util.List;

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

  /** One line of an exchange: who says it, and what they say. */
  public record Line(String speaker, String text) {}

  /**
   * The conversation Penny and Crazy Dave have when the player arrives in a chapter.
   *
   * <p>The doc asks for one or more NPCs exchanging several lines at the start of a level, not a
   * single line of advice, so Dave asks and Penny answers: the chapter's hazard gets explained in
   * the middle of a conversation rather than delivered as a briefing note.
   */
  public static List<Line> stageConversation(String seasonName, String playerName) {
    String player = playerName == null || playerName.isBlank() ? "Commander" : playerName.trim();
    switch (key(seasonName)) {
      case "egypt":
        return List.of(
            new Line(CRAZY_DAVE, "WABBY WABBO! Sand! Everywhere! Is this my back garden?"),
            new Line(PENNY, "This is Ancient Egypt, User Dave. Roughly five thousand years off."),
            new Line(CRAZY_DAVE, "So the zombies here are REALLY old zombies?"),
            new Line(PENNY, "Correct. And they hide under tombstones -- nothing will grow on "
                + "those, " + player + ". Clear them or plant around them."));
      case "frost":
        return List.of(
            new Line(CRAZY_DAVE, "Brrr! My taco froze solid! Again!"),
            new Line(PENNY, "Frostbite Caves. Ambient temperature: extremely inadvisable."),
            new Line(CRAZY_DAVE, "Penny, my PLANTS are turning into ice cubes!"),
            new Line(PENNY, "The icy winds do that, " + player + ". A frozen plant cannot act "
                + "until it thaws -- keep the lanes clear and watch the slippery tiles."));
      case "beach":
        return List.of(
            new Line(CRAZY_DAVE, "Beach party! Wait. Why is the lawn WET?"),
            new Line(PENNY, "Big Wave Beach. The tide advances with every zombie wave."),
            new Line(CRAZY_DAVE, "So my peashooters can swim, right? RIGHT?"),
            new Line(PENNY, "They cannot, User Dave. Only water plants survive the tide, "
                + player + " -- or a lily pad to stand on."));
      default:
        return List.of(
            new Line(CRAZY_DAVE, "It's dark. It's REALLY dark. Who turned off the sun?"),
            new Line(PENNY, "The Dark Ages, User Dave. It is night. It stays night."),
            new Line(CRAZY_DAVE, "Then where does my sun come from?!"),
            new Line(PENNY, "Nowhere, " + player + ". No sun falls at night -- grow your own, "
                + "and watch for graves raising fresh zombies."));
    }
  }

  /**
   * Shows an exchange one line at a time, then calls {@code onDone}.
   *
   * <p>Each line is its own popup titled with the speaker, so the player sees who is talking; the
   * chain is driven by the previous popup's own close, which is also what keeps the match paused
   * for exactly as long as the conversation lasts.
   */
  public static void showConversation(Stage stage, Skin skin, List<Line> lines, Runnable onDone) {
    if (stage == null || skin == null || lines == null || lines.isEmpty()) {
      if (onDone != null) {
        onDone.run();
      }
      return;
    }
    showFrom(stage, skin, lines, 0, onDone);
  }

  private static void showFrom(Stage stage, Skin skin, List<Line> lines, int index,
      Runnable onDone) {
    if (index >= lines.size()) {
      if (onDone != null) {
        onDone.run();
      }
      return;
    }
    Line line = lines.get(index);
    Label text = new Label(line.text(), skin, UiSkinProvider.LABEL_MEDIUM);
    text.setWrap(true);
    Table body = new Table();
    body.add(text).width(440f);
    boolean last = index == lines.size() - 1;
    Popup.show(stage, skin, line.speaker(), body,
        new Popup.Choice(last ? "Let's go" : "Next", UiSkinProvider.BUTTON_GREEN,
            () -> showFrom(stage, skin, lines, index + 1, onDone)));
  }
}
