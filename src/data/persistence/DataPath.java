package data.persistence;

import java.nio.file.Path;
import java.util.HashMap;

public class DataPath {
    private static final HashMap<String, Path> allPaths = new HashMap<>();
    private static final DataPath instance = new DataPath();

    private DataPath() {
        initialize();
    }

    private void initialize() {
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
