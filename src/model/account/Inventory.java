package model.account;

import java.util.HashMap;
import java.util.Map;

public class Inventory {
    private Map<String, Integer> items;
    private static final int MAX_PLANT_FOOD = 3;
    private static final int MAX_POTS = 20;

    public Inventory() {
        this.items = new HashMap<>();
    }

    public void addItem(String itemId, int quantity) {
        if (itemId == null || quantity <= 0) {
            return;
        }

        int currentQuantity = items.getOrDefault(itemId, 0);

        if ("plant_food".equals(itemId)) {
            items.put(itemId, Math.min(currentQuantity + quantity, MAX_PLANT_FOOD));

        } else if ("pot".equals(itemId)) {
            items.put(itemId, Math.min(currentQuantity + quantity, MAX_POTS));

        } else {
            items.put(itemId, currentQuantity + quantity);
        }
    }


    public boolean consumeItem(String itemId, int quantity) {
        int currentQuantity = items.getOrDefault(itemId, 0);

        if (currentQuantity >= quantity) {
            items.put(itemId, currentQuantity - quantity);
            return true;
        }
        return false;
    }


    public int getItemCount(String itemId) {
        return items.getOrDefault(itemId, 0);
    }

    public Map<String, Integer> getItems() {
        return items;
    }
}