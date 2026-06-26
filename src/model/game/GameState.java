package model.game;

import model.environment.Season;

public class GameState {
  private Wave currentWave;
  private Season currentSeason;
  private int currentSun;
  private int plantFoodCount;
  private int elapsedTime; // زمان سپری شده

  public GameState() {
    this.currentSun = 50;
    this.plantFoodCount = 0;
    this.elapsedTime = 0;
    this.currentWave = null;
    this.currentSeason = null;
  }

  public void update(Wave wave, Season season) {
    this.currentWave = wave;
    this.currentSeason = season;
    this.elapsedTime++; // افزایش زمان بازی به اندازه ۱ تیک
  }

  public void addSun(int amount) {
    if (amount > 0) {
      this.currentSun += amount;
      System.out.printf("Sun collected! Current Sun: %d%n", this.currentSun);
    }
  }

  public boolean deductSun(int amount) {
    if (this.currentSun >= amount) {
      this.currentSun -= amount;
      return true;
    }
    System.out.println("Error: Not enough sun!");
    return false;
  }

  public void addPlantFood() {
    this.plantFoodCount++;
    System.out.printf("Plant Food acquired! Total: %d%n", this.plantFoodCount);
  }

  public boolean usePlantFood() {
    if (this.plantFoodCount > 0) {
      this.plantFoodCount--;
      return true;
    }
    return false;
  }

  public Wave getCurrentWave() {
    return currentWave;
  }

  public Season getCurrentSeason() {
    return currentSeason;
  }

  public int getCurrentSun() {
    return currentSun;
  }

  public int getPlantFoodCount() {
    return plantFoodCount;
  }

  public int getElapsedTime() {
    return elapsedTime;
  }

  // تبدیل تیک به ثانیه
  public double getElapsedTimeInSeconds() {
    return this.elapsedTime / 10.0;
  }
}
