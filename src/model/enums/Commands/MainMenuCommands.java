package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum MainMenuCommands implements Command {
    Logout("menu\\s+logout");

    private final String pattern;

    MainMenuCommands(String pattern){this.pattern=pattern;}

    public Matcher getMatcher(String input) {
        Matcher matcher = Pattern.compile("\\s*" + this.pattern + "\\s*").matcher(input);

        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }
}
