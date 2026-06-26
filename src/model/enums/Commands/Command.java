package model.enums.Commands;

public interface Command {
  java.util.regex.Matcher getMatcher(String input);
}
