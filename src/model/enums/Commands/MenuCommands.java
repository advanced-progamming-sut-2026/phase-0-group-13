package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum MenuCommands implements Command {
  EnterMenu("menu\\s+enter\\s+(?<menuName>.+)"),
  ShowCurrentMenu("menu\\s+show\\s+current"),
  ExitMenu("menu\\s+exit");

  private final String pattern;

  MenuCommands(String pattern) {
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
