package model.plant.Factory;

import data.repository.PlantRepository;
import data.repository.ZombieRepository;
import model.enums.PlantType;
import model.plant.Plant;
import model.plant.PlantParts.PlantTemplate;

public class PlantFactory {
    private PlantRepository repository;
    public PlantFactory(PlantRepository repository) {
        this.repository = repository;
    }

    public Plant createPlant(String name, int row, int col) {
        PlantTemplate template = this.repository.find(name);
        if (template == null) {
            return null;
        }
        Plant livePlant = new Plant(template, row, col);
        return livePlant;
    }
}
