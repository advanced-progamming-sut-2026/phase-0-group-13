package model.game.minigame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.game.GameState;

public class PlantWhatYouGetRule extends MiniGame implements SpecialStageRule {
  private final List<String> deck;
  private final int rotationIntervalTicks;
  private final Random random = new Random();
  private int elapsedTicks;
  private String currentOffer;

  // برخلاف Conveyor (که رایگانه)، اینجا بازیکن هنوز باید هزینه خورشید بده؛ فقط انتخاب نمیکنه کدوم
  // گیاه، بازی بهش تحمیل میکنه (هر rotationIntervalTicks یکی رندوم از دک انتخاب میشه)
  public PlantWhatYouGetRule(List<String> deck, int rotationIntervalTicks) {
    this.deck = new ArrayList<>(deck);
    this.rotationIntervalTicks = rotationIntervalTicks;
    if (!this.deck.isEmpty()) {
      this.currentOffer = this.deck.get(random.nextInt(this.deck.size()));
    }
  }

  @Override
  public void apply(GameState gameState) {
    if (deck.isEmpty()) {
      return;
    }
    elapsedTicks++;
    if (elapsedTicks >= rotationIntervalTicks) {
      elapsedTicks = 0;
      currentOffer = deck.get(random.nextInt(deck.size()));
    }
  }

  @Override
  public boolean isPlantAllowed(String plantName) {
    return currentOffer != null && currentOffer.equalsIgnoreCase(plantName);
  }

  public String getCurrentOffer() {
    return currentOffer;
  }

  @Override
  public boolean checkWinCondition() {
    return false;
  }
}
