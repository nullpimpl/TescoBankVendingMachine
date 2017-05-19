package vendingMachine;

/**
 * Created by adobb on 2017-05-17.
 */
public class StockItem {
    public final char location;  // 'A'..'Z' inclusive
    public final int value;     // in pence
    public int quantityRemaining;
    public StockItem(char location, int value, int loadedQuantity) {
        this.location = location;
        this.value=value;
        this.quantityRemaining=loadedQuantity;
    }
}
