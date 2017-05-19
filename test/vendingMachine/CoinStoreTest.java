package vendingMachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Created by adobb on 2017-05-17.
 */
public class CoinStoreTest  {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testEmpty() {
        CoinStore store = new CoinStore();
        assertEquals(0, store.getValue());

        store = new CoinStore(0, 0, 0, 0);
        assertEquals(0, store.getValue());
    }

    @Test
    public void testInitialValue() {
        CoinStore store = new CoinStore(11, 7, 5, 3);
        assertEquals(1580, store.getValue());
    }

    @Test
    public void testAddCoin() {
        CoinStore store = new CoinStore(11, 7, 5, 3);
        assertEquals(1580, store.getValue());

        store.addCoin(CoinStore.Coin.POUND);
        assertEquals(1680, store.getValue());

        store.addCoin(CoinStore.Coin.FIFTY);
        assertEquals(1730, store.getValue());

        store.addCoin(CoinStore.Coin.TWENTY);
        assertEquals(1750, store.getValue());

        store.addCoin(CoinStore.Coin.TEN);
        assertEquals(1760, store.getValue());
    }

    @Test
    public void testAddCoinByValue() {
        CoinStore store = new CoinStore(11, 7, 5, 3);
        assertEquals(1580, store.getValue());

        store.addCoin(100);
        assertEquals(1680, store.getValue());

        store.addCoin(50);
        assertEquals(1730, store.getValue());

        store.addCoin(20);
        assertEquals(1750, store.getValue());

        store.addCoin(10);
        assertEquals(1760, store.getValue());
    }

    @Test
    public void testAddBadCoin() {
        CoinStore store = new CoinStore(11, 7, 5, 3);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("7");
        store.addCoin(7);
    }

    @Test
    /**
     * Test that for a given "shop float", we can find change for (find a set of coins equating to) various values
     */
    public void testCanFindCorrectChange() {
        CoinStore store = new CoinStore();
        assertTrue(store.canFindCorrectChange(0));  // find coins for 0 value from a balance of 0

        store = new CoinStore(11, 7, 5, 0); // £15.50
        final int initialVal = store.getValue();
        assertEquals(1550, initialVal);

        assertTrue(store.canFindCorrectChange(0));      // can always find correct coins for zero value
        assertFalse(store.canFindCorrectChange(3));     // 3 pence is never going to be possible without 1p and 2p being supported
        assertTrue(store.canFindCorrectChange(20));     // We have five 20p coins
        assertFalse(store.canFindCorrectChange(10));    // We have no 10p coins
        assertFalse(store.canFindCorrectChange(30));    // We still have no 10p coins
        assertTrue(store.canFindCorrectChange(store.getValue()));     // We can completely empty the bank
        assertTrue(store.canFindCorrectChange(store.getValue()-20));     // We can nearly empty the bank (leave 20p)
        assertFalse(store.canFindCorrectChange(store.getValue()-10));    // But we can't empty the bank to within 10p

        assertEquals(initialVal, store.getValue()); // No updates were made to the balance in all above queries
    }

    /**
     * Private helper method - combines repeatable tests of finding the correct change,
     * and updating the store's balance and coins as we go.
     * @param store - The CoinStore to get change from (will be updated)
     * @param pence - The value to get in change
     * @param expectPounds - The assert-expected number of £1 coins the change should be made up of
     * @param expectFifties - The assert-expected number of 50p coins the change should be made up of
     * @param expectTwenties - The assert-expected number of 20p coins the change should be made up of
     * @param expectTens - The assert-expected number of 10p coins the change should be made up of
     */
    private void testChange(CoinStore store, int pence, int expectPounds, int expectFifties, int expectTwenties, int expectTens) {
        final int expectedAfterVal = store.getValue()-pence;
        final int initialCoinCount = store.getCoinCount();
        CoinStore change = store.getChange(pence);
        assertEquals(pence, change.getValue());
        assertEquals(expectedAfterVal, store.getValue());
        assertEquals(expectPounds+expectFifties+expectTwenties+expectTens, change.getCoinCount());
        assertEquals(expectPounds, change.getCoinCount(CoinStore.Coin.POUND));
        assertEquals(expectFifties, change.getCoinCount(CoinStore.Coin.FIFTY));
        assertEquals(expectTwenties, change.getCoinCount(CoinStore.Coin.TWENTY));
        assertEquals(expectTens, change.getCoinCount(CoinStore.Coin.TEN));
        assertEquals(initialCoinCount-change.getCoinCount(), store.getCoinCount());
    }

    @Test
    public void testGetChange_InsufficientFunds() {
        final CoinStore store = new CoinStore(11, 7, 5, 3);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("balance");
        testChange(store, store.getValue()+10, 1, 1, 1, 1);
    }

    @Test
    /** Might be dangerous to ask for negative change */
    public void testGetChange_NegativeAmount() {
        final CoinStore store = new CoinStore(11, 7, 5, 3);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("negative");
        testChange(store, -1, 1, 1, 1, 1);
    }

    @Test
    /** Test that the minimum number of coins is used to make up any amount of change.
     * Including having to use extra smaller denomination coins when necessary
     */
    public void testGetChange() {
        testChange(new CoinStore(), 0, 0, 0, 0, 0);

        final CoinStore store = new CoinStore(11, 7, 5, 3);
        final int initialCoinCount = store.getCoinCount();
        final int initialVal = store.getValue();

        //These use the CoinStore copy constructor, so no balance updates persist
        //(Per line balance updates are tested each time within testChange)
        testChange(new CoinStore(store), 10, 0, 0, 0, 1);
        testChange(new CoinStore(store), 20, 0, 0, 1, 0);
        testChange(new CoinStore(store), 30, 0, 0, 1, 1);
        testChange(new CoinStore(store), 40, 0, 0, 2, 0);
        testChange(new CoinStore(store), 50, 0, 1, 0, 0);
        testChange(new CoinStore(store), 100, 1, 0, 0, 0);
        testChange(new CoinStore(store), 110, 1, 0, 0, 1);
        testChange(new CoinStore(store), 500, 5, 0, 0, 0);
        testChange(new CoinStore(store), 1400, 11, 6, 0, 0); // Not enough £1 coins, so raid the 50's stock
        testChange(new CoinStore(store), 1410, 11, 6, 0, 1);
        testChange(new CoinStore(store), initialVal, 11, 7, 5, 3); // Empty the whole bank
    }




}
