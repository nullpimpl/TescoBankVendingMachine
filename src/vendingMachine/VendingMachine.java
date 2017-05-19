package vendingMachine;

/**
 * Encapsulates the state of a vending vendingMachine and the operations that can be performed on it
 * Maintains a vendingMachine coin balance, a user entered coin balance, and vendingMachine held stock.
 * Constructs from a user-provided loader, which passes in the initial stock configuration and "shop float".
 * User error/warning messages (eg, "insufficient funds") are written to stderr
 * Info/success messages (eg sold item, returning change) are written to stdout
 */
public class VendingMachine {
    private CoinStore userBalance = new CoinStore();
    private final CoinStore bank;
    private final StockItem stock[]; // Up to 26 (A..Z inclusive locations) stock items. Any location can be null if no item configured for it.
    private boolean isOn=false;         // Current running state
    public static class InvalidStateException extends Exception{}

    /**
     * Constructor - configures the vendingMachine with the ILoader specified stock, and an initial float of change.
     * Using ILoader interface as an injectable mock to ease testing.
     * @param loader - Mockable interface to load the initial stock configuration from.
     * @param initialBalance - Initial "shop float" of change to initialise the "bank" with
     * @throws IllegalArgumentException
     */
    public VendingMachine(IVendingMachineStockLoader loader, CoinStore initialBalance) throws IllegalArgumentException {
        stock = new StockItem[validateItemPos('Z')+1]; // 26, A=0, Z=25, all default to null
        StockItem item;
        int itemCount=0;    // Total quantity of stock items loaded into the vendingMachine (eg, 5 coke bottles + 2 kitkats)
        while ((item=loader.getNextItem())!=null) {
            final int idx = validateItemPos(item.location);
            if (stock[idx]!=null) {
                throw new IllegalArgumentException("Multiple items configured at location "+item.location);
            }
            if (!CoinStore.isValidPrice(item.value)) {
                throw new IllegalArgumentException("Item at location " + item.location + " has an invalid price of "+item.value +'p');
            }
            stock[idx] = item;  // Add the stock itenm at its configured location
            itemCount += item.quantityRemaining;
        }
        if (itemCount==0) {
            throw new IllegalArgumentException("Machine is empty!");
        }
        this.bank = initialBalance;
    }

    /**
     * Has the vendingMachine got any stock left for sale?
     */
    private boolean isEmpty() {
        for (int i = 0; i< stock.length; i++) {
            if (stock[i]!=null && stock[i].quantityRemaining>0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validate that a location code is valid (A..Z inclusive), and convert it to a zero-based index
     * @param location Caharacter code, eg 'A'
     * @return zero-based index into stock[] array, eg 0 for 'A', 25 for 'Z'
     */
    private int validateItemPos(char location) {
        if (location<'A' || location>'Z') {
            throw new IllegalArgumentException("Invalid item location error (" + location + ')');
        }
        return location - 'A';
    }

    /** Test the current balance, with the current user balance added, to see if the combined
     * store hold enough coins to make up the given change.  This can be used in advance of vending
     * an item, to check that the user will not be short-changed.  Internally uses copies of the bank
     * and user balances, so that no updates are made to either.
     * @param change Value to check we will be able to provide
     * @return Boolean true iff we will be able to provide change
     */
    private boolean canFindCorrectChange(int change) {
        CoinStore test = new CoinStore(bank);           // A disposable test *copy* of the bank
        test.addCoins(new CoinStore(userBalance));      // Test add, moving from a *copy* of the user balance
        return test.canFindCorrectChange(change);
    }

    /** Superfluous accessors to demonstrate acutual usage - to be removed */
    public CoinStore getA() { return vendItem('A'); }
    public CoinStore getB() { return vendItem('B'); }
    public CoinStore getC() { return vendItem('C'); }

    /**
     * GET an item, from a given loaction, by way of selling it, and return the user's change
     * @param location - Upper case alpha location character code, eg 'A', 'B', 'C'
     * @return The amount of change given on success, or null on fail
     */
    public CoinStore vendItem(char location) {
        int idx = validateItemPos(location);
        StockItem item = stock[idx];
        if (item==null || item.quantityRemaining<=0) {
            System.err.print("That item is out of stock, please try another item.\n");
            return null;
        }

        if (userBalance.getValue()<item.value) {
            System.err.printf("Those cost %dp, you are %dp short\n", item.value, item.value-userBalance.getValue());
            return null;
        }

        item.quantityRemaining--;
        int change = userBalance.getValue() - item.value;

        // What if funds available is okay, but we don't have coins to give change>
        if (!canFindCorrectChange(change)) {
            System.err.print("Sorry, we don't have the right change to make that sale. Please add smaller coins, or choose another item.\n");
            return null;
        }

        // MOVE all coins into one store to aid finding optimum change (userBalance -> zero)
        bank.addCoins(userBalance);

        // NOTE: For simplicity, user can only buy one item at a time, change is automatically given upon vend.
        // What would be the alternative? A forgotten user press of coinReturn() when all done?
        CoinStore changeCoins = bank.getChange(change);
        System.out.printf("Sold a %3dp valued item, leaving %d of them, returning change=%s\n", item.value, item.quantityRemaining, changeCoins.toString());

        // So what if this last sale emptied the whole vendingMachine?
        // May as well switch off at that point?
        if (isEmpty()) {
            System.err.print("Last sale just emptied the vendingMachine, transitioning to OFF state\n");
            setOff();
        }

        return changeCoins; // CoinStore defining change for the mechine to eject
    }

    /**
     * @return User balance in pence
     */
    public int getUserBalance() {
        return userBalance.getValue();
    }

    /** Machine is switched ON */
    public boolean isOn() {
        return isOn;
    }

    /** Turn the vendingMachine ON */
    public void setOn() {
        // could throw an exception if switching on when already running,
        // but there is nothing to indicate that's required, and it may just be an annoyance.
        isOn = true;
    }

    /** Turn the vendingMachine OFF */
    public void setOff() {
        isOn = false;
    }

    /**
     * User insertion of coins, add to the userBalance
     * @param pence - Characteristics of the coin being entered - just using pence for now
     * @return Boolean true if the coin has been accepted.
     *                 If returns false, mechanics of vendingMachine should eject the coin
     * @throws InvalidStateException - if not currently allowed to accept coin input, eg, not running, or bad coin value
     */
    public boolean insertMoney(int pence) throws InvalidStateException {
        if (!isOn()) {                  // Don't accept coin input if we are switched off (okay, this should be mechanically impossible)
            throw new InvalidStateException();
        }

        try {
            userBalance.addCoin(pence);
            return true;
        }
        catch(IllegalArgumentException e) {
            // This would also be the scenario of a foreign coin being inserted.
            System.err.printf("Invalid coin inserted, returning coin, current balance is £%1.02f", userBalance.getValue()/100.0);
        }
        return false;   // eject (don't accept) the coin
    }

    /** If a user abandons their purchase, just return their coins as-was
     * Do not add the coins to the bank first, that could be exploited
     * as a change-normalising abuse of the vendingMachine.
     */
    public void coinReturn() {
        System.out.printf("Cancelled vend, returning %s\n", userBalance.toString());
        userBalance = new CoinStore();
    }
}
