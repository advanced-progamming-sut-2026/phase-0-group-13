package data.persistence;

import java.nio.file.Path;
import java.util.HashMap;

public class DataPath {
    private static HashMap<String, Path> AllPaths;

    private DataPath() {
    }

    public HashMap<String, Path> getAllPaths() {
        return AllPaths;
    }

    public Path getPath(String name) {
        return AllPaths.get(name);
    }
}
