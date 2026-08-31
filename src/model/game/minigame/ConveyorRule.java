package model.game.minigame;

import java.util.ArrayList;
import java.util.List;
import model.game.GameState;

public class ConveyorRule extends MiniGame implements SpecialStageRule {
  private final List<String> beltPlants;
  private final int spawnIntervalTicks;
  private final java.util.Random random = new java.util.Random();
  private int elapsedTicks;
  private String readyPlant;
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
    String next = beltPlants.get(random.nextInt(beltPlants.size()));
    if (beltPlants.size() > 1 && next.equals(lastDelivered)) {
      next = beltPlants.get((beltPlants.indexOf(next) + 1) % beltPlants.size());
    }
    lastDelivered = next;
    readyPlant = next;
    System.out.printf("The conveyor belt delivered a %s.%n", readyPlant);
  }
  @Override
  public ConveyorRule belt() {
    return this;
  }

  @Override
  public boolean isPlantAllowed(String plantName) {
    return readyPlant != null && plantName != null
            && model.account.User.normalizePlantKey(readyPlant)
            .equals(model.account.User.normalizePlantKey(plantName));
  }

  public String peekReadyPlant() {
    return readyPlant;
  }

  public List<String> getBeltPlants() {
    return java.util.Collections.unmodifiableList(beltPlants);
  }

  public String consumeReadyPlant() {
    String plant = readyPlant;
    readyPlant = null;
    return plant;
  }

  @Override
  public boolean checkWinCondition() {
    return false;
  }
}
