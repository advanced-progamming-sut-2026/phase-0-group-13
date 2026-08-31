package data.persistence;

import java.nio.file.Files;
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
    registerDataFile("plants", "plants.json");
    registerDataFile("zombies", "Zombies.json");
    registerDataFile("quests", "Quests.json");
    registerDataFile("games", "Games.json");
    registerDataFile("users", "Users.json");
    registerDataFile("session", "session.json");
  }

  private void registerDataFile(String name, String fileName) {
    registerPath(name, resolveDataFile(fileName));
  }

  private static Path resolveDataFile(String fileName) {
    Path[] candidates = {
      Paths.get("src", "data", "database", fileName),
      Paths.get("data", "database", fileName),
    };
    for (Path candidate : candidates) {
      if (Files.exists(candidate)) {
        return candidate;
      }
    }
    for (Path candidate : candidates) {
      if (Files.isDirectory(candidate.getParent())) {
        return candidate;
      }
    }
    return candidates[0];
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
