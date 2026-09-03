package model.game.minigame;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import model.game.GameState;

public class ConveyorRule extends MiniGame implements SpecialStageRule {

  /**
   * How many delivered cards the belt can hold before it stops feeding.
   *
   * <p>A belt that never fills up would hand the player every plant in the level for free; one
   * that held a single card threw the previous one away every time it delivered, which is what it
   * used to do.
   */
  public static final int BELT_CAPACITY = 10;

  private final List<String> beltPlants;
  private final int spawnIntervalTicks;
  private final java.util.Random random = new java.util.Random();
  private int elapsedTicks;
  /** The cards actually on the belt, oldest at the head. */
  private final Deque<String> delivered = new ArrayDeque<>();
  private String lastDelivered;

  public ConveyorRule(List<String> beltPlants, int spawnIntervalTicks) {
    this.beltPlants = new ArrayList<>(beltPlants);
    this.spawnIntervalTicks = spawnIntervalTicks;
  }

  @Override
  public void apply(GameState gameState) {
    if (beltPlants.isEmpty()) {
      return;
    }
    elapsedTicks++;
    if (elapsedTicks >= spawnIntervalTicks) {
      deliverNow();
    }
  }

  // داک: گیاه‌ها به‌صورت رندوم روی نوار می‌آیند. قبلا به ترتیب ثابت می‌چرخید، که یعنی بازیکن
  public void deliverNow() {
    if (beltPlants.isEmpty()) {
      return;
    }
    elapsedTicks = 0;
    // A full belt stops rather than dropping the card at its head, so nothing the player has been
    // given ever disappears without them planting it.
    if (delivered.size() >= BELT_CAPACITY) {
      return;
    }
    String next = beltPlants.get(random.nextInt(beltPlants.size()));
    if (beltPlants.size() > 1 && next.equals(lastDelivered)) {
      next = beltPlants.get((beltPlants.indexOf(next) + 1) % beltPlants.size());
    }
    lastDelivered = next;
    delivered.addLast(next);
    System.out.printf("The conveyor belt delivered a %s.%n", next);
  }

  @Override
  public ConveyorRule belt() {
    return this;
  }

  /**
   * Whether this plant is one the belt has actually handed over.
   *
   * <p>Any card on the belt, not only the one at the head: the belt is a queue the player picks
   * from, and refusing everything behind the head made the cards behind it decoration.
   */
  @Override
  public boolean isPlantAllowed(String plantName) {
    if (plantName == null) {
      return false;
    }
    String wanted = model.account.User.normalizePlantKey(plantName);
    for (String card : delivered) {
      if (model.account.User.normalizePlantKey(card).equals(wanted)) {
        return true;
      }
    }
    return false;
  }

  public String peekReadyPlant() {
    return delivered.peekFirst();
  }

  /** Every card on the belt right now, oldest first -- what the player can actually plant. */
  public List<String> getDeliveredPlants() {
    return List.copyOf(delivered);
  }

  /** The plants this belt draws from, which is not the same as what it has handed over. */
  public List<String> getBeltPlants() {
    return java.util.Collections.unmodifiableList(beltPlants);
  }

  public String consumeReadyPlant() {
    return delivered.pollFirst();
  }

  /**
   * Takes one named card off the belt.
   *
   * <p>Planting used to pop the head whichever card had been played, so planting anything but the
   * oldest card removed the wrong one from the belt.
   *
   * @return the card removed, or null if the belt was not holding one
   */
  public String consume(String plantName) {
    if (plantName == null) {
      return null;
    }
    String wanted = model.account.User.normalizePlantKey(plantName);
    for (java.util.Iterator<String> it = delivered.iterator(); it.hasNext();) {
      String card = it.next();
      if (model.account.User.normalizePlantKey(card).equals(wanted)) {
        it.remove();
        return card;
      }
    }
    return null;
  }

  public boolean isFull() {
    return delivered.size() >= BELT_CAPACITY;
  }

  @Override
  public boolean checkWinCondition() {
    return false;
  }
}
