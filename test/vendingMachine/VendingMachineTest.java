package vendingMachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.LinkedList;

class MockLoader implements IVendingMachineStockLoader {
    private final LinkedList<StockItem> items=new LinkedList<StockItem>();  // A fifo queue, equivalent of a walking file pointer

    public void addItem(char location, int value, int loadedQuantity) {
        items.add(new StockItem(location, value, loadedQuantity));
    }

    // In a real (non-mock) implementation, this might be reading a configuration file of some kind
    public StockItem getNextItem() {
        if (items.isEmpty()) {
            return null;
        }
        return items.pop();
    }
}

/**
 * Unit tests for {@link VendingMachine}
 */
public class VendingMachineTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    MockLoader mockLoader() {
        MockLoader loader = new MockLoader();
        loader.addItem('A', 60, 2);
        loader.addItem('B', 100, 2);
        loader.addItem('C', 170, 2);
        return loader;
    }

    @Test
    public void testConstructNoDefs(){
        thrown.expect(IllegalArgumentException.class);
        // TODO: Using expectMessage(substr) is fragile - would perhaps be better to define more exception types within VendingMachine?
        thrown.expectMessage("empty");
        VendingMachine machine = new VendingMachine(new MockLoader(), new CoinStore());
    }

    @Test
    public void testConstructEmpty(){
        MockLoader mockLoader = new MockLoader();
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("empty");
        VendingMachine machine = new VendingMachine(mockLoader, new CoinStore());
    }

    @Test
    public void testConstructDuplicateLocation() {
        MockLoader mockLoader = new MockLoader();
        mockLoader.addItem('A', 10, 5); // 2 items configured to same location 0,0
        mockLoader.addItem('A', 20, 3);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("ultiple items");
        VendingMachine machine = new VendingMachine(mockLoader, new CoinStore());
    }

    @Test
    public void testConstructInvalidLocation() {
        MockLoader mockLoader = new MockLoader();
        mockLoader.addItem('a', 10, 5);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("item location");
        VendingMachine machine = new VendingMachine(mockLoader, new CoinStore());
    }

    @Test
    public void testConstructInvalidItemPrice() {
        CoinStore.Coin coins[] = CoinStore.Coin.values();
        int minCoin = coins[coins.length-1].pence;  // Coin definitions arranged in descending value order
        if (minCoin>1) {                            // Test is only valid while 1p is not supported
            MockLoader mockLoader = new MockLoader();
            // Assuming minCoin is 10p, try creating an item with value 9p
            mockLoader.addItem('A', minCoin-1, 5);
            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage("invalid price");
            VendingMachine machine = new VendingMachine(mockLoader, new CoinStore());
        }
    }

    @Test
    public void defaultStateIsOff() {
        VendingMachine machine = new VendingMachine(mockLoader(),  new CoinStore());
        assertFalse(machine.isOn());
    }

    @Test
    public void turnsOn() {
        VendingMachine machine = new VendingMachine(mockLoader(), new CoinStore());
        machine.setOn();
        assertTrue(machine.isOn());
    }

    @Test
    public void testIsOn() throws VendingMachine.InvalidStateException {
        VendingMachine machine = new VendingMachine(mockLoader(), new CoinStore());
        thrown.expect(VendingMachine.InvalidStateException.class);
        machine.insertMoney(100);
    }

    @Test
    public void testVendOkay() throws Exception{
        VendingMachine machine = new VendingMachine(mockLoader(), new CoinStore(5, 5, 1, 5));
        machine.setOn();
        machine.insertMoney(100);

        CoinStore change = machine.vendItem('A');  // £1 credit, 60p sale -> 40p change
        assertEquals(40, change.getValue());
        assertEquals(1, change.getCoinCount(CoinStore.Coin.TWENTY)); // one 20p coin, plus two 10p coins
        assertEquals(2, change.getCoinCount(CoinStore.Coin.TEN));
        assertEquals(0, machine.getUserBalance());  // No balance after a successful vend
    }


    @Test
    public void testInsufficientFunds() throws Exception{
        VendingMachine machine = new VendingMachine(mockLoader(), new CoinStore(5, 5, 5, 5));
        machine.setOn();
        machine.insertMoney(100);
        CoinStore change = machine.vendItem('C');
        assertTrue(null==change);           // Vend fails, balance should remain

        change = machine.vendItem('A');    // Test balance remained by buying something cheaper
        assertTrue(change!=null);           // success
    }

    @Test
    public void testInsufficientChange() throws Exception{
        VendingMachine machine = new VendingMachine(mockLoader(), new CoinStore());
        machine.setOn();
        machine.insertMoney(100);       // Only coin in the whole vendingMachine, is this £1 coin
        CoinStore change = machine.vendItem('A');  // 60p item
        assertTrue(null==change);           // Vend fails (insufficiemt change), balance should remain

        change = machine.vendItem('B');    // Try to buy a more expensive item instead, but exact coin match (no change required)
        assertTrue(change!=null);           // success
    }


    @Test
    /** Test that an item can sell out, leaving the user able to select another item instead */
    public void testItemSellsOut() throws Exception{
        VendingMachine machine = new VendingMachine(mockLoader(), new CoinStore(100,100,100,100));
        machine.setOn();

        CoinStore change;
        machine.insertMoney(100);
        change = machine.vendItem('A');        // 2->1 item remaining
        assertTrue(change!=null);

        machine.insertMoney(100);
        change = machine.vendItem('A');        // 1->0 item remaining
        assertTrue(change!=null);

        machine.insertMoney(100);
        change = machine.vendItem('A');        // Should be none left
        assertTrue(change==null);               // vend fails, balance should remain

        change = machine.vendItem('B');        // But there are 2 of the £1 items left, and we still have £1 credit
        assertTrue(change!=null);               // Vend is a success
        assertEquals(0, change.getValue());     // Change is 0p
    }

    @Test
    public void testMachineSellsOut() throws Exception{
        VendingMachine machine = new VendingMachine(mockLoader(), new CoinStore(100,100,100,100));
        machine.setOn();

        // Sell out A
        machine.insertMoney(100);   machine.vendItem('A');
        machine.insertMoney(100);   machine.vendItem('A');

        // Sell out B
        machine.insertMoney(100);   machine.vendItem('B');
        machine.insertMoney(100);   machine.vendItem('B');

        // Sell out C
        machine.insertMoney(100);   machine.insertMoney(100);   machine.vendItem('C');
        assertTrue(machine.isOn());
        machine.insertMoney(100);   machine.insertMoney(100);   machine.vendItem('C');
        assertFalse(machine.isOn());
    }

    @Test
    public void testCoinReturn() throws Exception{
        VendingMachine machine = new VendingMachine(mockLoader(), new CoinStore());
        machine.setOn();

        machine.insertMoney(100);
        machine.insertMoney(50);
        assertEquals(150, machine.getUserBalance());
        machine.coinReturn();
        assertEquals(0, machine.getUserBalance());
    }

}
