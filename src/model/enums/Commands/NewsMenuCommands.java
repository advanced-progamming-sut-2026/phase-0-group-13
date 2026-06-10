package model.enums.Commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum NewsMenuCommands {
    ShowUnread("menu\\s+news\\s+show-unread"),
    ShowAll("menu\\s+news\\s+show-all");

    private final String pattern;

    NewsMenuCommands(String pattern){this.pattern=pattern;}

    public Matcher getMatcher(String input) {
        Matcher matcher = Pattern.compile("\\s*" + this.pattern + "\\s*").matcher(input);

        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }
}
