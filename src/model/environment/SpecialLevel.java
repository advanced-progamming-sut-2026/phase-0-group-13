package model.environment;

import model.enums.PlantType;

import java.util.List;

public class SpecialLevel {
    private String levelName;
    private List<PlantType> presetPlants;
    private int targetScore;

    public SpecialLevel(String levelName, int targetScore) {
        this.levelName = levelName;
        this.targetScore = targetScore;
    }

    public void checkWinCondition() {
    }
}

