package model.account;

import model.enums.PlantType;

import java.util.ArrayList;
import java.util.List;

public class Collection {
    private List<PlantType> unlockedPlants;
    private List<PlantType> lockedPlants;
    private List<PlantType> selectedDeck;

    public Collection() {
        this.unlockedPlants = new ArrayList<>();
        this.lockedPlants = new ArrayList<>();
        this.selectedDeck = new ArrayList<>();
    }


    public void unlockPlant(PlantType plant) {
    }

    public void addToDeck(PlantType plant) {
    }

}
