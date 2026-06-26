package model.core;

import controller.BaseController;
import java.util.Scanner;

public class Router {
  private static Router instance;
  private BaseController currentController;
  private Scanner scanner;

  private Router() {
    this.scanner = new Scanner(System.in);
  }

  public static Router getInstance() {
    if (instance == null) {
      instance = new Router();
    }
    return instance;
  }

  public void navigateTo(BaseController newController) {
    if (currentController != null) {
      currentController.exit();
    }

    this.currentController = newController;

    if (this.currentController != null) {
      this.currentController.initController();
      startInputLoop();
    }
  }

  private void startInputLoop() {
    while (currentController != null) {
      if (scanner.hasNextLine()) {
        String input = scanner.nextLine();
        currentController.handleinput(input);
      }
    }
  }

  public void close() {
    this.currentController = null;
    System.out.println("Router closed.");
  }
}
