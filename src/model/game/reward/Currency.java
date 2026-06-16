package model.game.reward;

public class Currency {
    private int coins;
    private int diamonds;

    public Currency() {
    }

    public Currency(int coins, int diamonds) {
        this.coins = coins;
        this.diamonds = diamonds;
    }

    public void addCoins(int amount) {
        coins += amount;
    }

    public void addDiamonds(int amount) {
        diamonds += amount;
    }
}
