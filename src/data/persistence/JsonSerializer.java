package data.persistence;

import com.google.gson.Gson;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class JsonSerializer {
  private static final Gson GSON = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

  public void writeToFile(String filePath, Object data) {
    File target = new File(filePath);
    try {
      File parentDir = target.getParentFile();
      if (parentDir != null && !parentDir.exists()) {
        parentDir.mkdirs();
      }
      File tempDir = parentDir != null ? parentDir : new File(".");
      File tmpFile = File.createTempFile("userdata", ".tmp", tempDir);
      try (Writer writer = new FileWriter(tmpFile)) {
        GSON.toJson(data, writer);
      }
      Files.move(tmpFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      System.out.println("error: could not save data to " + filePath + " (" + e.getMessage() + ")");
    }
  }

  public <Type> Type readFromFile(String filePath, Class<Type> c) {
    try (Reader reader = new FileReader(filePath)) {
      return GSON.fromJson(reader, c);
    } catch (FileNotFoundException e) {
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}