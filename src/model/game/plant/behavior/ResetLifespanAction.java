package model.game.plant.behavior;

import model.game.Board;
import model.game.plant.Plant;

/**
 * اثر غذای گیاهِ Sea-shroom و Puff-shroom: طبق plants.json «یک رگبار شلیک می‌کند <b>و عمر هر
 * Sea-shroom/Puff-shroom را از نو می‌کند</b>». بخش دومش اصلا پیاده نشده بود.
 *
 * <p>رگبار را به اکشن اصلی (شوتر) واگذار می‌کند و خودش فقط عمرها را ریست می‌کند، تا از همان
 * ShootForwardAction موجود دوباره استفاده شود و منطق شلیک تکرار نشود.
 */
public class ResetLifespanAction implements PlantAction {

  private final PlantAction burst;

  public ResetLifespanAction(PlantAction burst) {
    this.burst = burst;
  }

  @Override
  public void execute(Plant plant, Board board, int currentTick) {
    if (burst != null) {
      burst.execute(plant, board, currentTick);
    }

    int refreshed = 0;
    for (Plant other : board.getPlants()) {
      if (!other.isDead() && other.getLifespanTicks() > 0
              && other.getName().equalsIgnoreCase(plant.getName())) {
        other.resetLifespan(currentTick);
        refreshed++;
      }
    }
    if (refreshed > 0) {
      System.out.printf("%s refreshed the lifespan of %d %s(s).%n",
              plant.getName(), refreshed, plant.getName());
    }
  }
}
