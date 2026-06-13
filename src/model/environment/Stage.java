package model.environment;

import model.enums.StageType;

public class Stage {
    private String id;

    private String name;

    private Season season;

    private StageType type;

    private boolean unlocked;
    private Stage nextStage;

    private boolean completed;
}
