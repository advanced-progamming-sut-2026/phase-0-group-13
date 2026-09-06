package data.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class DataPath {
  private static final HashMap<String, Path> ALLPATHS = new HashMap<>();
  /** Where the read-only templates sit inside the jar. */
  private static final String PACKED_DIR = "/database/";
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
    return playerDataFile(fileName);
  }

  /**
   * Where the data lives when there is no project tree to read: a folder of the player's own,
   * seeded once from the copy packed into the jar.
   *
   * <p>Only the templates travel in the jar. Saves, accounts and the signed-in session are
   * personal, so they are not packed and simply start out absent -- every reader here already
   * treats a missing file as "nothing saved yet".
   */
  private static Path playerDataFile(String fileName) {
    Path folder = Paths.get(System.getProperty("user.home", "."), ".pvz", "database");
    Path target = folder.resolve(fileName);
    if (Files.exists(target)) {
      return target;
    }
    try (InputStream packed = DataPath.class.getResourceAsStream(PACKED_DIR + fileName)) {
      Files.createDirectories(folder);
      if (packed != null) {
        Files.copy(packed, target);
      }
    } catch (IOException e) {
      System.out.println("error: could not lay out the game data in " + folder
          + " (" + e.getMessage() + ")");
    }
    return target;
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
