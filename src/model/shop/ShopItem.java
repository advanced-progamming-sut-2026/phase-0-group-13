package model.shop;

import model.enums.PlantType;

public class ShopItem {
    private String id;
    private int price;
    private int stock;
    private PlantType plantType;

    public ShopItem() {
    }

    public ShopItem(String id, int price, int stock, PlantType plantType) {
        this.id = id;
        this.price = price;
        this.stock = stock;
        this.plantType = plantType;
    }

    public int getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return stock > 0;
    }
}
