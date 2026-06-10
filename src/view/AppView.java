package view;

import model.core.App;

import java.util.Scanner;

public class AppView {
    public static void run() {
        Scanner scanner = new Scanner(System.in);
        do {
            if (!scanner.hasNextLine()) break;
            App.getCurrentMenu().checkCommand(scanner);
        }while (scanner.hasNextLine() && !App.shouldExit);
    }
}
