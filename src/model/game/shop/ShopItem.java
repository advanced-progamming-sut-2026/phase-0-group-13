package model.game.shop;

import model.enums.PlantType;

public class ShopItem {
    private String id;
    private int price;
    private int stock;
    private PlantType plantType;
    private boolean daily ;



    public ShopItem() {
    }

    public ShopItem(String id, int price, int stock, PlantType plantType, boolean daily) {
        this.id = id;
        this.price = price;
        this.stock = stock;
        this.plantType = plantType;
        this.daily = daily;
    }
    public int getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return stock > 0;
    }
}
