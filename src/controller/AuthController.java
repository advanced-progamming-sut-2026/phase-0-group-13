package controller;

import data.persistence.UserManager;
import model.Result;
import model.enums.Commands.MainMenuCommands;
import model.enums.Commands.MenuCommands;
import model.enums.Commands.SignInMenuCommands;
import model.enums.Commands.SignUpMenuCommands;

import java.util.regex.Matcher;

public class AuthController implements BaseController {

    @Override
    public void initController() {
        System.out.println("Entered Auth Menu.");
    }

    @Override
    public void handleinput(String command) {
        Matcher matcher;

        if ((matcher = SignUpMenuCommands.Register.getMatcher(command)) != null) {
            handleRegister(matcher);
        }
        else if ((matcher = SignUpMenuCommands.PickQuestion.getMatcher(command)) != null) {
            handlePickQuestion(matcher);
        }
        else if ((matcher = SignInMenuCommands.Login.getMatcher(command)) != null) {
            handleLogin(matcher);
        }
        else if ((matcher = MenuCommands.EnterMenu.getMatcher(command)) != null) {
            String targetMenu = matcher.group("menuName").trim().toLowerCase();
            handleEnterMenu(targetMenu);
        }
        else if ((matcher = SignInMenuCommands.ForgetPassword.getMatcher(command)) != null) {
            handleForgetPassword(matcher);
        }
        else if ((matcher = SignInMenuCommands.Answer.getMatcher(command)) != null) {
            handleAnswer(matcher);
        }
        else if (command.trim().equals("menu show current")) {
            System.out.println("Auth Menu");
        }
        else if (command.trim().equals("menu exit")) {
            exit();
        }
        else {
            System.out.println("invalid command");
        }
    }

    private void handleRegister(Matcher matcher) {
        String username = matcher.group("username");
        String password = matcher.group("password");
        String passwordConfirm = matcher.group("passwordConfirm");
        String nickname = matcher.group("nickname");
        String email = matcher.group("email");
        String gender = matcher.group("gender");

        if (!password.equals(passwordConfirm)) {
            System.out.println("error: passwords do not match");
            return;
        }

        try {
            UserManager.getInstance().registerUser(username, password, nickname, email, gender);
            System.out.println("User data syntax is valid. Proceeding to pick a security question...");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void handlePickQuestion(Matcher matcher) {
        String qNumber = matcher.group("questionNumber");
        String answer = matcher.group("answer");
        String answerConfirm = matcher.group("answerConfirm");

        if (!answer.equals(answerConfirm)) {
            System.out.println("error: answers do not match");
            return;
        }

        try {
            System.out.println("Security question picked successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleLogin(Matcher matcher) {
        String username = matcher.group("username");
        String password = matcher.group("password");

        boolean stayLoggedIn = matcher.group("stay") != null;

        try {
            UserManager.getInstance().loginUser(username, password, stayLoggedIn);
            System.out.println("Login successful!");

            model.core.App.setCurrentMenu(model.enums.Menu.MainMenu);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleForgetPassword(Matcher matcher) {
        String username = matcher.group("username");
        String email = matcher.group("email");

        try {
            String question = UserManager.getInstance().initiatePasswordRecovery(username, email);
            System.out.println(question);
            System.out.println("Forget password initiated for " + username);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleAnswer(Matcher matcher) {
        String answer = matcher.group("answer");

        try {
            UserManager.getInstance().verifyRecoveryAnswer(answer);
            System.out.println("Checking security answer...");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void exit() {
        System.out.println("Exiting application...");
        System.exit(0);
    }

    private void handleEnterMenu(String targetMenu) {
        model.enums.Menu current = model.core.App.getCurrentMenu();

        if (current == model.enums.Menu.SignUpMenu || current == model.enums.Menu.SignInMenu) {
            if (targetMenu.equals("sign in") || targetMenu.equals("signin")) {
                model.core.App.setCurrentMenu(model.enums.Menu.SignInMenu);
                System.out.println("Entered Sign In Menu.");
                return;
            }
            else if (targetMenu.equals("sign up") || targetMenu.equals("signup")) {
                model.core.App.setCurrentMenu(model.enums.Menu.SignUpMenu);
                System.out.println("Entered Sign Up Menu.");
                return;
            }
            else {
                System.out.println("please login first!");
                return;
            }
        }

    }
}