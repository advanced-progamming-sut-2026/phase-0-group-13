package model.game.plant.behavior;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

/**
 * Caulipower: در plants.json «یک شلیک جادویی (جهت تصادفی، از موانع رد می‌شود، هدفش را هیپنوتیزم
 * می‌کند)» ثبت شده و اثر غذای گیاهش «چند زامبی تصادفی روی زمین را هیپنوتیزم می‌کند».
 *
 * <p>قبلا با HomingAction و دمیج ۱۰۰۰۰ ساخته می‌شد، یعنی دقیقا برعکسِ دیتا هدف را می‌کُشت.
 *
 * <p>برخلاف {@link HypnoShroomAction} که منتظر می‌ماند زامبی بخوردش، این یکی از راه دور و روی
 * هدف‌های تصادفی کار می‌کند (پس ردیف و مانع برایش مهم نیست).
 */
public class HypnotiseAction implements PlantAction {

  private final int actionInterval;
  private final int targets;

  public HypnotiseAction(int actionInterval, int targets) {
    this.actionInterval = Math.max(1, actionInterval);
    this.targets = Math.max(1, targets);
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (currentTick - plant.getLastActionTick() < actionInterval) {
      return;
    }

    List<Zombie> candidates = new ArrayList<>();
    for (Zombie zombie : board.getZombies()) {
      if (!zombie.isDead() && !zombie.isHypnotized()) {
        candidates.add(zombie);
      }
    }
    if (candidates.isEmpty()) {
      return;
    }

    Collections.shuffle(candidates);
    int hypnotised = Math.min(targets, candidates.size());
    for (int i = 0; i < hypnotised; i++) {
      Zombie zombie = candidates.get(i);
      zombie.setHypnotized(true);
      zombie.setEating(false);
      System.out.printf("%s hypnotised %s; it now fights for you!%n",
              plant.getName(), zombie.getName());
    }
    plant.setLastActionTick(currentTick);
  }
}
