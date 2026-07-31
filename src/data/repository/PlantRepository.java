package data.repository;

import java.util.List;
import model.game.plant.PlantParts.PlantTemplate;

public class PlantRepository implements ReadOnlyRepository<Object> {
  private final List<PlantTemplate> plants;

  public PlantRepository(List<PlantTemplate> plants) {
    this.plants = plants;
  }

  public List<PlantTemplate> getAll() {
    return plants;
  }

  public PlantTemplate find(String name) {
    if (name == null) {
      return null;
    }
    String key = normalize(name);
    for (PlantTemplate template : plants) {
      if (template.name != null && normalize(template.name).equals(key)) {
        return template;
      }
    }
    return null;
  }

  private static String normalize(String name) {
    return name.toLowerCase().replaceAll("[^a-z0-9]", "");
  }
}
