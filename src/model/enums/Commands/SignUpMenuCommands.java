package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum SignUpMenuCommands implements Command {
  Register("^\\s*register\\s+-u\\s+(?<username>\\S+)" +
          "\\s+-p\\s+(?<password>\\S+)\\s+(?<passwordConfirm>\\S+)\\s+-n" +
          "\\s+(?<nickname>\\S+)\\s+-e\\s+(?<email>\\S+)\\s+-g\\s+(?<gender>\\S+)\\s*$"),
  PickQuestion("^\\s*pick\\s+question\\s+-q" +
          "\\s+(?<questionNumber>\\S+)\\s+-a\\s+(?<answer>.+?)\\s+-c" +
          "\\s+(?<answerConfirm>.+)\\s*$");

  private final String pattern;

  SignUpMenuCommands(String pattern) {
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