package data.repository;

import model.plant.Plant;
import model.plant.PlantParts.PlantTemplate;

import java.util.List;

public class PlantRepository implements ReadOnlyRepository<Object> {
    private final List<PlantTemplate> plants;

    public PlantRepository(List<PlantTemplate> plants) {
        this.plants = plants;
    }

    public List<PlantTemplate> getAll() {
        return plants;
    }
    public PlantTemplate find(String name) {
        for (PlantTemplate template : plants) {
            if (template.name.equalsIgnoreCase(name)) {
                return template;
            }
        }
        return null;
    }
}
