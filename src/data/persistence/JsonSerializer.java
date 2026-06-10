package data.persistence;

import com.google.gson.Gson;

import java.io.*;

public class JsonSerializer {
    private static final Gson gson = new Gson();

    public static void writeToFile(String filePath, Object data) {
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <Type> Type readFromFile(String filePath, Class<Type> c) {
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
