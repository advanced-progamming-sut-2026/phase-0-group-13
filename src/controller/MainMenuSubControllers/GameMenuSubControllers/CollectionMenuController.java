package controller.MainMenuSubControllers.GameMenuSubControllers;

import controller.BaseController;
import data.GameDataManager;
import data.persistence.UserManager;
import java.util.List;
import java.util.regex.Matcher;
import model.Result;
import model.account.User;
import model.core.App;
import model.enums.Commands.CollectionMenuCommands;
import model.enums.Commands.MenuCommands;
import model.enums.Menu;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.zombie.ZombieParts.ZombieTemplate;

public class CollectionMenuController implements BaseController {

  private static final String ZOMBIE_PREFIX = "zombie_";
  private static final int PURCHASE_COST_COINS = 2000;
  private static final int UPGRADE_COIN_COST = 500;
  private static final int UPGRADE_SEED_COST = 10;
  private static final int MAX_PLANT_LEVEL = 4;

  @Override
  public void initController() {}

  @Override
  public void handleinput(String command) {
    Matcher matcher;

    if (CollectionMenuCommands.ShowPlants.getMatcher(command) != null) {
      handleShowPlants();
    } else if (CollectionMenuCommands.ShowAllPlants.getMatcher(command) != null) {
      handleShowAllPlants();
    } else if (CollectionMenuCommands.ShowZombies.getMatcher(command) != null) {
      handleShowZombies();
    } else if (CollectionMenuCommands.ShowAllZombies.getMatcher(command) != null) {
      handleShowAllZombies();
    } else if ((matcher = CollectionMenuCommands.ShowPlant.getMatcher(command)) != null) {
      handleShowPlant(matcher.group("plantName").trim());
    } else if ((matcher = CollectionMenuCommands.ShowZombie.getMatcher(command)) != null) {
      handleShowZombie(matcher.group("zombieName").trim());
    } else if ((matcher = CollectionMenuCommands.UpgradePlant.getMatcher(command)) != null) {
      handleUpgradePlant(matcher.group("plantName").trim());
    } else if ((matcher = CollectionMenuCommands.PurchasePlant.getMatcher(command)) != null) {
      handlePurchasePlant(matcher.group("plantName").trim());
    } else if (MenuCommands.ShowCurrentMenu.getMatcher(command) != null) {
      System.out.println("Collection Menu");
    } else if (MenuCommands.ExitMenu.getMatcher(command) != null) {
      exit();
    } else {
      System.out.println("invalid input");
    }
  }

  private User requireUser() {
    User user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      System.out.println("error: no user logged in");
    }
    return user;
  }

  private void handleShowPlants() {
    User user = requireUser();
    if (user == null) return;

    if (user.getUnlockedPlants().isEmpty()) {
      System.out.println("You haven't unlocked any plants yet.");
      return;
    }

    System.out.println("--- Your Plants ---");
    for (String name : user.getUnlockedPlants()) {
      PlantTemplate template = findPlantTemplate(name);
      String displayName = template != null ? template.name : name;
      System.out.println("  " + displayName + " (level " + user.getPlantLevel(name) + ")");
    }
  }

  private void handleShowAllPlants() {
    List<PlantTemplate> all = allPlants();
    if (all == null || all.isEmpty()) {
      System.out.println("No plant data available.");
      return;
    }

    User user = UserManager.getInstance().getCurrentUser();
    System.out.println("--- All Plants ---");
    for (PlantTemplate template : all) {
      boolean unlocked = user != null && user.hasUnlockedPlant(template.name);
      System.out.println("  " + template.name + " - " + (unlocked ? "unlocked" : "locked"));
    }
  }

  private void handleShowZombies() {
    User user = requireUser();
    if (user == null) return;

    if (user.getUnlockedZombies().isEmpty()) {
      System.out.println("You haven't encountered any zombies yet.");
      return;
    }

    System.out.println("--- Zombies You've Seen ---");
    for (String rawName : user.getUnlockedZombies()) {
      ZombieTemplate template = findZombieTemplate(stripZombiePrefix(rawName));
      System.out.println("  " + (template != null ? template.name : stripZombiePrefix(rawName)));
    }
  }

  private void handleShowAllZombies() {
    List<ZombieTemplate> all = allZombies();
    if (all == null || all.isEmpty()) {
      System.out.println("No zombie data available.");
      return;
    }

    User user = UserManager.getInstance().getCurrentUser();
    System.out.println("--- All Zombies ---");
    for (ZombieTemplate template : all) {
      boolean seen = user != null && user.getUnlockedZombies().contains(zombieKey(template.name));
      System.out.println("  " + template.name + " - " + (seen ? "seen" : "not yet encountered"));
    }
  }

  private void handleShowPlant(String plantName) {
    PlantTemplate template = findPlantTemplate(plantName);
    if (template == null) {
      System.out.println("error: unknown plant: " + plantName);
      return;
    }

    User user = UserManager.getInstance().getCurrentUser();
    boolean unlocked = user != null && user.hasUnlockedPlant(template.name);

    System.out.println("Name: " + template.name);
    System.out.println("Cost: " + template.cost + " sun");
    System.out.println("Health: " + template.baseHp);
    System.out.println("Category: " + template.category);
    System.out.println("Tags: " + template.tags);
    System.out.println("Ability: " + template.baseAbility);
    System.out.println(
        "Status: "
            + (unlocked ? "unlocked (level " + user.getPlantLevel(template.name) + ")" : "locked"));
  }

  private void handleShowZombie(String zombieName) {
    ZombieTemplate template = findZombieTemplate(zombieName);
    if (template == null) {
      System.out.println("error: unknown zombie: " + zombieName);
      return;
    }

    User user = UserManager.getInstance().getCurrentUser();
    boolean seen = user != null && user.getUnlockedZombies().contains(zombieKey(template.name));

    System.out.println("Name: " + template.name);
    System.out.println("Type: " + template.type);
    System.out.println("Health: " + template.baseHp);
    System.out.println("Speed: " + template.baseSpeed);
    System.out.println("Special abilities: " + template.specialAbilities);
    System.out.println("Status: " + (seen ? "seen" : "not yet encountered"));
  }

  private void handleUpgradePlant(String plantName) {
    User user = requireUser();
    if (user == null) return;

    if (!user.hasUnlockedPlant(plantName)) {
      System.out.println("error: you haven't unlocked " + plantName + " yet");
      return;
    }

    PlantTemplate template = findPlantTemplate(plantName);
    String canonicalName =
        template != null ? template.name.toLowerCase() : plantName.toLowerCase().trim();

    int currentLevel = user.getPlantLevel(canonicalName);
    if (currentLevel >= MAX_PLANT_LEVEL) {
      System.out.println("error: " + plantName + " is already at the maximum level");
      return;
    }

    String seedKey = "seed_" + canonicalName;
    if (user.getInventory().getItemCount(seedKey) < UPGRADE_SEED_COST) {
      System.out.println(
          "error: not enough seed packets for " + plantName + " (need " + UPGRADE_SEED_COST + ")");
      return;
    }
    if (user.getCoins() < UPGRADE_COIN_COST) {
      System.out.println("error: not enough coins (need " + UPGRADE_COIN_COST + ")");
      return;
    }

    user.getInventory().consumeItem(seedKey, UPGRADE_SEED_COST);
    user.addCoins(-UPGRADE_COIN_COST);

    int newLevel = currentLevel + 1;
    user.setPlantLevel(canonicalName, newLevel);

    System.out.println(plantName + " upgraded to level " + newLevel + "!");
    printLevelAbility(template, newLevel);
    saveState();
  }

  private void printLevelAbility(PlantTemplate template, int level) {
    if (template == null) return;

    String description;
    switch (level) {
      case 2:
        description = template.lvl2;
        break;
      case 3:
        description = template.lvl3;
        break;
      case 4:
        description = template.lvl4;
        break;
      default:
        description = null;
    }

    if (description != null && !description.isBlank()) {
      System.out.println("New ability: " + description);
    }
  }

  private void handlePurchasePlant(String plantName) {
    User user = requireUser();
    if (user == null) return;

    PlantTemplate template = findPlantTemplate(plantName);
    if (template == null) {
      System.out.println("error: unknown plant: " + plantName);
      return;
    }

    if (user.hasUnlockedPlant(template.name)) {
      System.out.println("error: you already own " + template.name);
      return;
    }

    if (user.getCoins() < PURCHASE_COST_COINS) {
      System.out.println("error: not enough coins (need " + PURCHASE_COST_COINS + ")");
      return;
    }

    user.addCoins(-PURCHASE_COST_COINS);
    Result result = user.unlockPlant(template.name);
    System.out.println(result.message());

    if (result.success()) {
      saveState();
    } else {
      user.addCoins(PURCHASE_COST_COINS);
    }
  }

  private void saveState() {
    try {
      UserManager.getInstance().updateCurrentUserGameState();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private List<PlantTemplate> allPlants() {
    return GameDataManager.plantRepository != null
        ? GameDataManager.plantRepository.getAll()
        : null;
  }

  private List<ZombieTemplate> allZombies() {
    return GameDataManager.zombieRepository != null
        ? GameDataManager.zombieRepository.getAll()
        : null;
  }

  private PlantTemplate findPlantTemplate(String name) {
    return GameDataManager.plantRepository != null
        ? GameDataManager.plantRepository.find(name.trim())
        : null;
  }

  private ZombieTemplate findZombieTemplate(String name) {
    return GameDataManager.zombieRepository != null
        ? GameDataManager.zombieRepository.find(name.trim())
        : null;
  }

  private String zombieKey(String templateName) {
    return ZOMBIE_PREFIX + templateName.toLowerCase();
  }

  private String stripZombiePrefix(String rawName) {
    return rawName.startsWith(ZOMBIE_PREFIX) ? rawName.substring(ZOMBIE_PREFIX.length()) : rawName;
  }

  @Override
  public void exit() {
    System.out.println("Changed to Game Menu.");
    App.setCurrentMenu(Menu.GameMenu);
  }
}
