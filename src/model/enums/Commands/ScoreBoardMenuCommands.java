package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ScoreBoardMenuCommands implements Command {
  SortByLastPassedLevel("sort\\s+by\\s+last\\s+passed\\s+level"),
  SortByMiniGames("sort\\s+by\\s+mini\\s+games"),
  SortByDailyQuests("sort\\s+by\\s+daily\\s+quests"),
  SortByQuests("sort\\s+by\\s+quests"),
  SortByHighScore("sort\\s+by\\s+high\\s+score");

  private final String pattern;

  ScoreBoardMenuCommands(String pattern) {
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
