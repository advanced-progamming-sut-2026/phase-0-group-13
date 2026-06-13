package model.environment;

import model.enums.PlantType;
import model.environment.AttackPatterns.AttackPattern;

import java.util.List;

public class SpecialLevel {
    private String levelName;
    private int targetScore;
    private List<PlantType> allowedPlants;
    private AttackPattern attackPattern;

    public SpecialLevel(String levelName, int targetScore) {
        this.levelName = levelName;
        this.targetScore = targetScore;
    }

    public void checkWinCondition() {
    }
}

