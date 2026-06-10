package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum QuestMenuCommands implements Command {
    TravelLogPage("travel\\s+log\\s+page\\s+(?<pageName>.+)");

    private final String pattern;

    QuestMenuCommands(String pattern){this.pattern=pattern;}

    public Matcher getMatcher(String input) {
        Matcher matcher = Pattern.compile("\\s*" + this.pattern + "\\s*").matcher(input);

        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }
}
