package controller;

import Model.Result;

public interface CommandHandler {
    Result handle(String command);
}
