package vendingMachine;

/**
 * Created by adobb on 2017-05-17.
 * CoinStore class encapsulates a number of coins being held, as a wallet (purse), running balance, change to give,
 * or a bank. Supported coins are defined in the Coin enumeration.
 */
public class CoinStore {
    public enum Coin {
        // Performance note - if adding many coins, or more properties of a coin to key on (other than just pence),
        // Coin.find() performance may benefit from re-writing to use a hash key and lookup.
        POUND(100), FIFTY(50), TWENTY(20), TEN(10);    // Descending value order, ordinal used as index
        public final int pence;

        // TODO: Could there be a maximum quantity of a coin that can be held?
        // TODO: Could there be a min/max quantity of a coin for the vendingMachine to endevour to hold (see getChange)?
        // TODO: Could have a CoinDesc class (weight, diameter, thickness, ...) instead of "int pence" as a key.

        private Coin(int pence) {
            this.pence=pence;
        }

        /**
         * @param pence - Characteristics of the coin to identify. Just using pence value for now.
         * @throws IllegalArgumentException if the coin is not recognised/supported
         * @return Coin identified
         */
        public static Coin find(int pence) throws IllegalArgumentException {
            for (Coin coin: Coin.values()) {        // Could do a binary chop, or hashtable keyed, but with 4 items, linear walk will always be faster
                if (coin.pence==pence) {
                    return coin;
                }
            }
            throw new IllegalArgumentException("Unrecognised coin (" + pence + ")");
        }
    }

    /** The counts of each coin currently held in the store, indexed by the Coin enum */
    private final int coinCount[] = new int[Coin.values().length];

    /** Default to an empty CoinStore */
    public CoinStore() {
        this(0,0,0,0);
    }

    /** Initialise a CoinStore with a certain set of coins (eg, shop float) */
    public CoinStore(int pounds, int fifeties, int twenties, int tens) {
        coinCount[Coin.POUND.ordinal()]=pounds;
        coinCount[Coin.FIFTY.ordinal()]=fifeties;
        coinCount[Coin.TWENTY.ordinal()]=twenties;
        coinCount[Coin.TEN.ordinal()]=tens;
    }

    /**
     * Copy constructor
     */
    public CoinStore(CoinStore oth) {
        for (int i=coinCount.length-1; i>=0; --i)
            coinCount[i]=oth.coinCount[i];
    }

    /**
     * Just for debug, string representation of the content of the CoinStore
     */
    @Override
    public String toString() {
        StringBuilder sb=new StringBuilder();
        for (Coin coin: Coin.values()) {
            sb.append(coin.name()).append('[').append(coinCount[coin.ordinal()]).append("];");
        }
        sb.append(String.format("total=£%1.02f", getValue()/100.0));
        return sb.toString();
    }

    /**
     * Get the total value of the CoinStore
     * @return total value in pence
     */
    public int getValue() {
        int value=0;
        for (Coin coin: Coin.values()) {
            value += coinCount[coin.ordinal()] * coin.pence;
        }
        return value;
    }

    /**
     * Total number of coins in the store, more useful for dubug and test validation.
     * @return Number of coins, regardless of denomination
     */
    public int getCoinCount() {
        int coins=0;
        for (Coin coin: Coin.values()) {
            coins += coinCount[coin.ordinal()];
        }
        return coins;
    }

    /**
     * Debug/test method, query the store to see how many of a given coin are held
     */
    public int getCoinCount(Coin coin) {
        return coinCount[coin.ordinal()];
    }

    /**
     * Add a single coin of given value to the store balance (checks is a recognized coin)
     * @param pence Characteristics of the coin to identify it - just pence value for now
     * @throws IllegalArgumentException if coin is not recognised
     */
    public void addCoin(int pence) throws IllegalArgumentException {
        addCoin(Coin.find(pence));
    }

    /**
     * Add a single coin of given value to the store balance
     */
    public void addCoin(Coin coin) {
        coinCount[coin.ordinal()]++;
    }

    /**
     * Add the contents of another CoinStore to this CoinStore,
     * *moving* that value into this CoinStore.
     */
    public void addCoins(CoinStore entered) {
        for (Coin coin: Coin.values()) {
            coinCount[coin.ordinal()] += entered.coinCount[coin.ordinal()];
            entered.coinCount[coin.ordinal()] = 0; // A coin cannot exist in two stores at once, this is a MOVE operation
        }
    }

    /**
     * Test this CoinStore to see if exact change can be found for the given value
     * @param value in pence
     * @return Boolean true if exact change is available
     */
    public boolean canFindCorrectChange(int value) {
        return getChange(value, false).getValue() == value;
    }

    /**
     * Gets the user-optimal change (least coins) from this CoinStore,
     * updating the balance of this CoinStore
     * @param value Pence value to find change for
     * @return Another CoinStore, defining the change
     */
    public CoinStore getChange(int value) {
        return getChange(value, true);
    }

    /**
     * Check that a given price is valid for our set of defined coins.
     * eg: if our smallest supported coin is 10p, then a price of anything
     * not exactly divisible by 10p, cannot be supported.
     * @param pence Test price
     * @return boolean true if price is valid
     */
    public static boolean isValidPrice(int pence) {
        Coin coinDefs[]=Coin.values();
        return pence % coinDefs[coinDefs.length-1].pence == 0;
    }

    /**
     * Gets the user-optimal (minimal coin-count) set of coins from this store, to represent the given value.
     * TODO: There may be other more complex strategies - for example if the CoinStore wants to only keep between 100 and 250 TEN pence coins?
     * @param pence  Value to find change for
     * @param updateBalance - boolean true iff the returned store's coins, should be removed from *this store
     * @return Another CoinStore, defining the change to give
     */
    private CoinStore getChange(int pence, boolean updateBalance) {
        if (getValue()<pence) { // This should be an impossible error, hence IllegalArgumentException
            throw new IllegalArgumentException("Cannot ask for more change than is in the current balance");
        }
        if (pence<0) { // Potentially dangerous request, should be an impossible error, hence IllegalArgumentException
            throw new IllegalArgumentException("Cannot ask for negative change");
        }

        CoinStore change = new CoinStore();
        for (Coin coin: Coin.values()) {    // descending value order of coins
            int req = pence / coin.pence;   // Ideal required quantity of this coin for change (truncated integer divide)
            int use = Math.min(req, coinCount[coin.ordinal()]); // Best we can do from available quantity of this coin
            change.coinCount[coin.ordinal()] = use;
            pence -= use * coin.pence;
            if (updateBalance) {
                coinCount[coin.ordinal()] -= use;   // Remove the coins from *this store
            }
        }
        // What if value>0, ie, we have insufficient change in the bank for the customer?
        // Have to give the customer the best change we can, but no more than they are due.
        // So don't throw an exception (could, and call addCoins(change) to undo the failed op),
        // but added canFindCorrectChange to preempt this soft error instead.

        return change;
    }
}
