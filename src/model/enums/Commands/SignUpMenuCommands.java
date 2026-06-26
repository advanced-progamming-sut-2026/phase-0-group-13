package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum SignUpMenuCommands implements Command {
  Register(
      "register\\s+-u\\s+(?<username>\\S+)\\s+-p\\s+(?<password>\\S+)\\s+(?<passwordConfirm>\\S+)\\s+-n\\s+" +
              "(?<nickname>\\S+)\\s+-e\\s+(?<email>\\S+)\\s+-g\\s+" +
              "(?<gender>.+)"),
  PickQuestion(
      "pick\\s+question\\s+-q\\s+(?<questionNumber>\\S+)\\s+-a\\s+(?<answer>\\S+)\\s+-c\\s+(?<answerConfirm>.+)");

  private final String pattern;

  SignUpMenuCommands(String pattern) {
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
