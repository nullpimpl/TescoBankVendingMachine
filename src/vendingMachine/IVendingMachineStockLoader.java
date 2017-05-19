package vendingMachine;

/**
 * Created by adobb on 2017-05-17.
 * On construct of a VendingMachine instance, this passes in the initial stock configuration.
 */
public interface IVendingMachineStockLoader {
    /**
     * Return the next vending item definition, or null if no more items
     */
    public StockItem getNextItem();
}
