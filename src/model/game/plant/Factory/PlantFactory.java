package model.game.plant.Factory;

import data.repository.PlantRepository;
import model.game.plant.Plant;
import model.game.plant.PlantParts.PlantTemplate;

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
