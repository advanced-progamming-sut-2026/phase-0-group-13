package data;

import data.persistence.DataPath;
import data.persistence.JsonSerializer;
import data.persistence.UserManager;
import data.repository.*;
import data.repository.PlantRepository;
import java.util.Arrays;
import java.util.List;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.quest.Quest;
import model.game.zombie.ZombieParts.ZombieTemplate;

public class GameDataManager {

  public static PlantRepository plantRepository;
  public static QuestRepository questRepository;
  public static ZombieRepository zombieRepository;

  private static boolean sessionRestored = false;

  public GameDataManager() {
    initAllData();
  }

  private static boolean isLoaded = false;

  public void initAllData() {
    if (isLoaded) {
      return;
    }
    JsonSerializer serializer = new JsonSerializer();
    DataPath dataPath = DataPath.getInstance();

    try {
      if (dataPath.getPath("plants") != null) {
        String plantsFilePath = dataPath.getPath("plants").toString();
        PlantTemplate[] plantTemplates =
                serializer.readFromFile(plantsFilePath, PlantTemplate[].class);

        if (plantTemplates != null) {
          List<PlantTemplate> plantList = Arrays.asList(plantTemplates);
          plantRepository = new PlantRepository(plantList);
        }
      }

      if (dataPath.getPath("quests") != null) {
        String questsFilePath = dataPath.getPath("quests").toString();
        Quest[] quests = serializer.readFromFile(questsFilePath, Quest[].class);
        if (quests != null) {
          List<Quest> questList = Arrays.asList(quests);
          questRepository = new QuestRepository(questList);
        }
      }

      if (dataPath.getPath("zombies") != null) {
        String zombiesFilePath = dataPath.getPath("zombies").toString();
        ZombieTemplate[] zombieTemplates =
                serializer.readFromFile(zombiesFilePath, ZombieTemplate[].class);
        if (zombieTemplates != null) {
          List<ZombieTemplate> zombieList = Arrays.asList(zombieTemplates);
          zombieRepository = new ZombieRepository(zombieList);
        }
      }

      sessionRestored = UserManager.getInstance().restoreSession();

      isLoaded = true;

    } catch (Exception e) {
      System.err.println("EROR,at initAllData" + e.getMessage());
      e.printStackTrace();
    }
  }

  public static boolean wasSessionRestored() {
    return sessionRestored;
  }

  public static void clearData() {
    plantRepository = null;
    questRepository = null;
    zombieRepository = null;
    sessionRestored = false;
    isLoaded = false;
  }
}