package model.plant.Factory;

import model.XY;
import model.enums.PlantType;
import model.plant.BasePlant;
import model.plant.LobberPlant;
import model.plant.*;

public class PlantFactory {
    private PlantFactory() {
    }

    public static BasePlant createPlant(PlantType plantType) {

        BasePlant plant = switch (plantType) {

            //case PEASHOOTER-> new ShooterPlant();
            case REPEATER -> null;
            case SUNFLOWER -> null;
            case WALL_NUT -> null;
            case TNT -> null;
            //case MINT -> new MintPlanet();
            case WALL_NUTS -> null;
            case CABBAGE_PULT -> null;
            case MELON_PULT -> null;
            case MELON -> null;
            case FUMESHROOM -> null;
            // اینجا هایی که نال گذاشتیم باید  در واقع از پکیج دیتا ، بریم اون چیز مربوطه رو برداریم
        };


        return plant;
    }
}
