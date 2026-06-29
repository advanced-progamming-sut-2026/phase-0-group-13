package data.persistence;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class DataPath {
  private static final HashMap<String, Path> ALLPATHS = new HashMap<>();
  private static final DataPath INSTANCE = new DataPath();

  private DataPath() {
    initialize();
  }

  public static DataPath getInstance() {
    return INSTANCE;
  }

  private void initialize() {
    registerPath("plants", Paths.get("data/database/plants.json"));
    registerPath("zombies", Paths.get("data/database/Zombies.json"));
    registerPath("quests", Paths.get("data/database/Quests.json"));
    registerPath("games", Paths.get("data/database/Games.json"));
    registerPath("users", Paths.get("data/database/Users.json"));
    registerPath("session", Paths.get("data/database/session.json"));
  }

  public HashMap<String, Path> getAllPaths() {
    return ALLPATHS;
  }

  public Path getPath(String name) {
    return ALLPATHS.get(name);
  }

  public void registerPath(String name, Path path) {
    ALLPATHS.put(name, path);
  }
}
