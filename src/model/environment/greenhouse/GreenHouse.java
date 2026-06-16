package model.environment.greenhouse;

import model.game.plant.Plant;

public class GreenHouse {
    private int capacity;
    private int time ;
    private PlantUpgrade plantUpgrade;

    public GreenHouse() {
        this(0);
    }

    public GreenHouse(int capacity) {
        this.capacity = capacity;
    }
    public int getCapacity() {
        return capacity;
    }
    public void setCapacity(int capacity) {}

    public void setTime(int time) {
        this.time = time;
    }

    public void showAllPlants() {
    }

    public void collectPlant() {
    }
    public void PlantMaking(Plant plant){
        // اینجا میایم گیاه جدید و میزاریم ( کلاس پلنت اپگرید هم هستش )
    }
}
