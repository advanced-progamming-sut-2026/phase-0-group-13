package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum GamePlayMenuCommands implements Command {
  AdvanceTime("advance\\s+time\\s+-t\\s+(?<count>\\S+)\\s+ticks"),
  CollectSun("collect\\s+sun\\s+-l\\s+\\((?<x>\\S+),\\s*(?<y>\\S+)\\)"),
  ShowSunAmount("show\\s+sun\\s+amount"),
  CheatAddSuns("cheat\\s+add\\s+-n\\s+(?<count>\\S+)\\s+suns"),
  ReleaseTheNuke("release\\s+the\\s+nuke"),
  PlantPlant("plant\\s+plant\\s+-t\\s+(?<type>\\S+)\\s+-l\\s+\\((?<x>\\S+),\\s*(?<y>\\S+)\\)"),
  CheatRemoveCooldown("cheat\\s+remove-cooldown"),
  PluckPlant("pluck\\s+plant\\s+-l\\s+\\((?<x>\\S+),\\s*(?<y>\\S+)\\)"),
  FeedPlant("feed\\s+plant\\s+-l\\s+\\((?<x>\\S+),\\s*(?<y>\\S+)\\)"),
  CheatAddPlantFood("cheat\\s+add-plant-food"),
  ShowMap("show\\s+map"),
  ShowPlantsStatus("show\\s+plants\\s+status"),
  ShowTileStatus("show\\s+tile\\s+status\\s+-l\\s+\\((?<x>\\S+),\\s*(?<y>\\S+)\\)"),
  ZombiesInfo("zombies\\s+info"),
  CheatSpawnZombie(
      "cheat\\s+spawn-zombie\\s+-t\\s+(?<zombieType>\\S+)\\s+-l\\s+<(?<x>\\S+),\\s*(?<y>\\S+)>"),
  StartZombieWaves("start\\s+zombie\\s+waves");

  private final String pattern;

  GamePlayMenuCommands(String pattern) {
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
