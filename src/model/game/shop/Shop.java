package model.game.shop;

import java.util.List;
import java.util.Objects;

public class Shop {
    private List<Objects> AllTimeProducts ;
    private List<Objects> DailyTimeProducts ;

    public void listItems() {
    }

    public void buyItem(String itemId, int count, String plantType) {
    }
    public List<Objects> getAllTimeProducts() {
        return AllTimeProducts;
    }
    public void AddAllTimeProducts() {}
    public void AddDailyProducts() {}


}
