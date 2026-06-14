package data.persistence;

import java.nio.file.Path;
import java.util.HashMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class DataPath {
    private static final HashMap<String, Path> allPaths = new HashMap<>();
    private static final DataPath instance = new DataPath();

    private DataPath() {
        initialize();
    }

    private void initialize() {
        registerPath("plants", Paths.get("src/main/resources/data/database/plants.json"));
        registerPath("zombies", Paths.get("src/main/resources/data/database/Zombies.json"));
        registerPath("quests", Paths.get("src/main/resources/data/database/Quests.json"));
        registerPath("games", Paths.get("src/main/resources/data/database/Games.json"));
        registerPath("users", Paths.get("src/main/resources/data/database/Users.json"));

    }

    public static DataPath getInstance() {
        return instance;
    }

    public HashMap<String, Path> getAllPaths() {
        return allPaths;
    }

    public Path getPath(String name) {
        return allPaths.get(name);
    }

    public void registerPath(String name, Path path) {
        allPaths.put(name, path);
    }
}