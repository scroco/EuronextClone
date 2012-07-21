package com.euronextclone;

import com.euronextclone.ordertypes.Limit;
import com.euronextclone.ordertypes.Peg;
import com.euronextclone.ordertypes.PegWithLimit;
import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.reactive.Reactive;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PegOrderLimitFill2TradesTest extends BaseReactiveTest {
    private void buyOrders(MatchingUnit matchingUnit) {
        matchingUnit.addOrder(Order.OrderSide.Buy, "A", 200, new Limit(11.5D));
        matchingUnit.addOrder(Order.OrderSide.Buy, "B", 150, new PegWithLimit(11.6D));
        matchingUnit.addOrder(Order.OrderSide.Buy, "B", 70, new Peg());
        matchingUnit.addOrder(Order.OrderSide.Buy, "B", 125, new Limit(10.5D));
    }

    private void sellOrders(MatchingUnit matchingUnit) {
        matchingUnit.addOrder(Order.OrderSide.Sell, "C", 130, new Limit(11.800000000000001D));
        matchingUnit.addOrder(Order.OrderSide.Sell, "C", 350, new Limit(11.9D));
        matchingUnit.addOrder(Order.OrderSide.Sell, "D", 275, new Limit(12D));
    }

    @Test
    public void newBuyAndSellOrderTest() {
        final MatchingUnit matchingUnit = new MatchingUnit();
        buyOrders(matchingUnit);
        sellOrders(matchingUnit);
        matchingUnit.addOrder(Order.OrderSide.Buy, "E", 200, new Limit(11.7));

        MatcherAssert.assertThat("Buy Order Depth", matchingUnit.orderBookDepth(Order.OrderSide.Buy), Matchers.is(5));
        MatcherAssert.assertThat("Sell Order Depth", matchingUnit.orderBookDepth(Order.OrderSide.Sell), Matchers.is(3));
    }

    @Test
    public void newSellLimitOrderTest() throws IOException {
        final MatchingUnit matchingUnit = new MatchingUnit();
        buyOrders(matchingUnit);
        sellOrders(matchingUnit);

        matchingUnit.addOrder(Order.OrderSide.Buy, "E", 200, new Limit(11.699999999999999D));

        final Closeable close = matchingUnit.register(Reactive.toObserver(new Action1<Trade>() {
            @Override
            public void invoke(Trade value) {
                int count = incTradeCount();

                switch (count) {
                    case 0:
                        assertThat(value.getPrice(), is(11.7));
                        assertThat(value.getSellBroker(), is("A"));
                        assertThat(value.getBuyBroker(), is("E"));
                        assertThat(value.getQuantity(), is(200));
                        break;
                    case 1:
                        assertThat(value.getPrice(), is(11.7));
                        assertThat(value.getSellBroker(), is("A"));
                        assertThat(value.getBuyBroker(), is("B"));
                        assertThat(value.getQuantity(), is(70));
                        break;
                }
            }
        }));

        matchingUnit.addOrder(Order.OrderSide.Sell, "A", 270, new Limit(11.699999999999999D));
        close.close();

        MatcherAssert.assertThat("Received Trade", getReceivedTradeCount(), is(2));
        MatcherAssert.assertThat("Buy Order Depth", matchingUnit.orderBookDepth(Order.OrderSide.Buy), Matchers.is(3));
        MatcherAssert.assertThat("Sell Order Depth", matchingUnit.orderBookDepth(Order.OrderSide.Sell), Matchers.is(3));
    }
}
