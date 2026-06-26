package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum CollectionMenuCommands implements Command {
  ShowPlants("menu\\s+collection\\s+show-plants"),
  ShowAllPlants("menu\\s+collection\\s+show-all-plants"),
  ShowZombies("menu\\s+collection\\s+show-zombies"),
  ShowAllZombies("menu\\s+collection\\s+show-all-zombies"),
  ShowPlant("menu\\s+collection\\s+show-plant\\s+-p\\s+(?<plantName>.+)"),
  ShowZombie("menu\\s+collection\\s+show-zombie\\s+-z\\s+(?<zombieName>.+)"),
  UpgradePlant("menu\\s+collection\\s+upgrade-plant\\s+-p\\s+(?<plantName>.+)"),
  PurchasePlant("menu\\s+collection\\s+purchase-plant\\s+-p\\s+(?<plantName>.+)");

  private final String pattern;

  CollectionMenuCommands(String pattern) {
    this.pattern = pattern;
  }

  public Matcher getMatcher(String input) {
    Matcher matcher = Pattern.compile("\\s*" + this.pattern + "\\s*").matcher(input);

    if (matcher.matches()) {
      return matcher;
    }
    return null;
  }
}
