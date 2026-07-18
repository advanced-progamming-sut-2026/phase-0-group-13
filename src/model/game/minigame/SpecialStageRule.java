package model.game.minigame;

import model.game.Board;
import model.game.GameState;

public interface SpecialStageRule {
  void apply(GameState gameState);

  // پیش‌فرض: هیچ محدودیتی روی کاشت گیاه نیست
  default boolean isPlantAllowed(String plantName) {
    return true;
  }

  // پیش‌فرض: شرط باخت اضافه‌ای نداره (شرط عادی بازی خودش جدا چک میشه)
  default boolean checkLoseCondition(Board board) {
    return false;
  }

  // پیش‌فرض: شرط برد زودهنگام اضافه‌ای نداره (برد عادی با تموم شدن موج‌ها خودش جدا چک میشه)
  default boolean checkWinCondition(Board board) {
    return false;
  }
}
