package model.game.minigame;

import model.game.Board;
import model.game.GameState;
import model.game.quest.MatchContext;

public class LoveYourPlantsRule extends MiniGame implements SpecialStageRule {
  private final int lossBudget;
  private final MatchContext context;

  // طبق داک: اگه به تعداد مشخصی گیاه از دست بره (خورده بشه یا نابود بشه) مرحله باخته میشه
  public LoveYourPlantsRule(int lossBudget, MatchContext context) {
    this.lossBudget = lossBudget;
    this.context = context;
  }

  @Override
  public void apply(GameState gameState) {}

  public int getLossBudget() { return lossBudget; }

  @Override
  public boolean checkLoseCondition(Board board) {
    return context != null && context.getPlantsLost() >= lossBudget;
  }

  @Override
  public boolean checkWinCondition() {
    return false;
  }
}
