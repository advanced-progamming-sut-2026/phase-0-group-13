package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum SignInMenuCommands implements Command {
  Login("^\\s*login\\s+-u\\s+(?<username>\\S+)\\s+-p\\s+(?<password>\\S+)(?<stay>\\s+-stay-logged-in)?\\s*$"),
  ForgetPassword("^\\s*forget\\s+password\\s+-u\\s+(?<username>\\S+)\\s+-e\\s+(?<email>\\S+)\\s*$"),
  Answer("^\\s*answer\\s+-a\\s+(?<answer>.+)\\s*$");

  private final String pattern;

  SignInMenuCommands(String pattern) {
    this.pattern = pattern;
  }

  public Matcher getMatcher(String input) {
    Matcher matcher = Pattern.compile(this.pattern).matcher(input);
    if (matcher.matches()) {
      return matcher;
    }
    return null;
  }
}