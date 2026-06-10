package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum SettingsMenuCommands implements Command {
    ChangeDifficulty("menu\\s+settings\\s+change-difficulty\\s+-l\\s+(?<difficultyLevel>.+)");

    private final String pattern;

    SettingsMenuCommands(String pattern){this.pattern=pattern;}

    public Matcher getMatcher(String input) {
        Matcher matcher = Pattern.compile("\\s*" + this.pattern + "\\s*").matcher(input);

        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }
}
