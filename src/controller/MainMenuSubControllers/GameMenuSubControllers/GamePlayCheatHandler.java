package controller.MainMenuSubControllers.GameMenuSubControllers;

import data.GameDataManager;
import data.persistence.UserManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.account.User;
import model.core.GameManager;
import model.core.MatchSetup;
import model.game.minigame.SpecialStageRule;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.zombie.Zombie;
import model.game.zombie.factory.ZombieFactory;

final class GamePlayCheatHandler {

  private GamePlayCheatHandler() {}

  static void addSuns(GameManager gm, int count) {
    if (count <= 0) {
      System.out.println("error: count must be a positive number");
      return;
    }
    gm.getBoard().getGameState().addSun(count);
    System.out.println("Added " + count + " suns.");
  }

  static void spawnZombie(GameManager gm, String type, int row, int col) {
    if (GameDataManager.zombieRepository == null) {
      System.out.println("error: zombie data is not loaded");
      return;
    }
    try {
      Zombie zombie = new ZombieFactory(GameDataManager.zombieRepository)
              .createZombie(resolveZombieAlias(type), row, col);
      if (zombie == null) {
        System.out.println("error: unknown zombie type '" + type + "'");
        return;
      }
      gm.getBoard().spawnZombie(zombie);
      System.out.println("Spawned zombie " + zombie.getDisplayName() + ".");
    } catch (RuntimeException e) {
      System.out.println("error: could not spawn zombie (data unavailable)");
    }
  }

  /** Accepts either the raw alias or the name the almanac shows. */
  private static String resolveZombieAlias(String typed) {
    String wanted = typed == null ? "" : typed.trim();
    if (GameDataManager.zombieRepository.find(wanted) != null) {
      return wanted;
    }
    for (var template : GameDataManager.zombieRepository.getAll()) {
      String display = template.getDisplayName();
      if (display != null && display.equalsIgnoreCase(wanted)) {
        return template.getName();
      }
    }
    return wanted;
  }

  static void unlockAllPlants(GameManager gm) {
    if (GameDataManager.plantRepository == null) {
      System.out.println("error: plant data is not loaded");
      return;
    }

    User user = UserManager.getInstance().getCurrentUser();
    List<PlantTemplate> catalogue = GameDataManager.plantRepository.getAll();
    List<String> everyPlant = new ArrayList<>();
    Set<String> alreadyOwned = new HashSet<>();

    if (user != null) {
      for (String owned : user.getUnlockedPlants()) {
        alreadyOwned.add(normalizePlantKey(owned));
      }
    }

    int newlyUnlocked = 0;
    for (PlantTemplate template : catalogue) {
      if (template.name == null || template.name.isBlank()) {
        continue;
      }
      everyPlant.add(template.name.toLowerCase());
      if (user != null && alreadyOwned.add(normalizePlantKey(template.name))) {
        user.unlockPlant(template.name);
        newlyUnlocked++;
      }
    }

    MatchSetup.getInstance().setSelectedPlants(everyPlant);
    gm.disableCooldowns();
    gm.enableFreePlanting();

    System.out.println("=== DEBUG: all plants unlocked ===");
    System.out.printf(
            "  %d plants available (%d newly added to your collection)%n",
            everyPlant.size(), newlyUnlocked);
    System.out.println("  sun cost      : waived for the rest of this match");
    System.out.println("  recharge      : disabled");
    System.out.println("  seed bank     : now lists every plant ('show plants status')");
    System.out.println("  plant anything: plant plant -t <name> -l (x, y)");
    warnIfSpecialRuleRestrictsPlanting(gm);
  }

  private static final String UNRESTRICTED_PROBE = "__unlisted_plant_probe__";

  private static void warnIfSpecialRuleRestrictsPlanting(GameManager gm) {
    SpecialStageRule rule = gm.getSpecialStageRule();
    if (rule == null || rule.isPlantAllowed(UNRESTRICTED_PROBE)) {
      return;
    }
    System.out.println(
            "  note          : this is a special level ("
                    + rule.getClass().getSimpleName()
                    + ") and it still restricts which plants may be placed.");
    System.out.println(
            "                  Run this cheat on a normal level (level 1 of a chapter) to place"
                    + " every plant.");
  }

  private static String normalizePlantKey(String name) {
    return name == null ? "" : name.toLowerCase().replaceAll("[^a-z0-9]", "");
  }

  static void releaseTheNuke(GameManager gm) {
    int killed = 0;
    for (Zombie z : gm.getBoard().getZombies()) {
      if (!z.isDead()) {
        z.takeDamage(1_000_000, true);
        killed++;
      }
    }
    System.out.println("Nuke released. Zombies wiped: " + killed);
    // امتیاز SIMULTANEOUS_KILL دیگه خودکار و عمومی از GameManager.processBoardEvents ثبت میشه
    // (وقتی چند زامبی تو یه تیک بمیرن)، پس اینجا دیگه لازم نیست جدا صداش بزنیم
  }
}
