package data.persistence;

import com.google.gson.Gson;

import java.io.*;

public class JsonSerializer {
    private static final Gson gson = new Gson();

    public void writeToFile(String filePath, Object data) {
        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(data, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public <Type> Type readFromFile(String filePath, Class<Type> c) {
        try (Reader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, c);
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
