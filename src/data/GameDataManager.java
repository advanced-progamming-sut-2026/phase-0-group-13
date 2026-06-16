package data;

import data.persistence.DataPath;
import data.persistence.JsonSerializer;
import data.repository.PlantRepository;
import data.repository.*;
import model.game.plant.PlantParts.PlantTemplate;
import model.game.quest.Quest;
import model.game.zombie.ZombieParts.ZombieTemplate;

import java.util.Arrays;
import java.util.List;

public class GameDataManager {

    public static PlantRepository plantRepository;
    public static QuestRepository questRepository;
    public static ZombieRepository zombieRepository;
    public GameDataManager(){
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
                PlantTemplate[] plantTemplates = serializer.readFromFile(plantsFilePath, PlantTemplate[].class);

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
                ZombieTemplate[] zombieTemplates = serializer.readFromFile(zombiesFilePath, ZombieTemplate[].class);
                if (zombieTemplates != null) {
                    List<ZombieTemplate> zombieList = Arrays.asList(zombieTemplates);
                    zombieRepository = new ZombieRepository(zombieList);
                }
                }
            isLoaded = true;

        } catch (Exception e) {
            System.err.println("سرور گوزید" + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void clearData() {
        plantRepository = null;
        questRepository = null;
        zombieRepository = null;
        isLoaded = false;
    }
}
