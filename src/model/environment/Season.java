package model.environment;

public class Season {
    private String name;
    private boolean hasPool;
    private boolean hasRoof;
    private boolean isNight;
    private int[][] gridPaths;

    public Season(String name, boolean hasPool, boolean hasRoof, boolean isNight) {
        this.name = name;
        this.hasPool = hasPool;
        this.hasRoof = hasRoof;
        this.isNight = isNight;
    }

    public void applyEnvironmentEffects() {
    }
}
