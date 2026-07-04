package model.environment.greenhouse;

public  class Pot {
    private boolean isUnlocked;   // وضعیت قفل بودن یا نبودن گلدان
    private String plantedSeedId;
    private long plantTime;

    public Pot(boolean isUnlocked) {
        this.isUnlocked = isUnlocked;
        this.plantedSeedId = null;
        this.plantTime = 0;
    }

    public void unlock() {
        this.isUnlocked = true;
    }

    public void plant(String seedId) {
        if (!isUnlocked) return; // اگر گلدان قفل است، اجازه کاشت نمی‌دهد
        this.plantedSeedId = seedId;
        this.plantTime = System.currentTimeMillis();
    }

    public void collect() {
        this.plantedSeedId = null;
        this.plantTime = 0;
    }

    public boolean isUnlocked() { return isUnlocked; }
    public boolean isEmpty() { return plantedSeedId == null; }
    public String getPlantedSeedId() { return plantedSeedId; }
    public long getPlantTime() { return plantTime; }
}