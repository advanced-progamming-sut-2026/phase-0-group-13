package model.account;

import model.Result;

public class PlayerProfile {
    private String username;
    private int coins;
    private Collection collection;
    private Progress progress;

    public PlayerProfile(String username) {
        this.username = username;
        this.coins = 0;
        this.collection = new Collection();
        this.progress = new Progress();
    }


    public Result addCoins(int amount) {
        if (amount <= 0) {
            return new Result(false, "Invalid coin amount.", null);
        }
        this.coins += amount;
        return new Result(true, amount + " coins added. Total: " + this.coins, this.coins);
    }

    public Result spendCoins(int amount) {
        if (amount <= 0) {
            return new Result(false, "Invalid coin amount.", null);
        }
        if (this.coins >= amount) {
            this.coins -= amount;
            return new Result(true, "Purchase successful. Remaining coins: " + this.coins, this.coins);
        }
        return new Result(false, "Not enough coins! You need " + (amount - this.coins) + " more.", this.coins);
    }

    public String getUsername() {
        return username;
    }

    public int getCoins() {
        return coins;
    }

    public Collection getCollection() {
        return collection;
    }

    public Progress getProgress() {
        return progress;
    }
}