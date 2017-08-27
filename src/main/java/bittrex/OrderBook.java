package bittrex;

import java.math.BigDecimal;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class OrderBook {

    
    public final TreeMap<BigDecimal, BigDecimal> asks = new TreeMap<>();
    public final TreeMap<BigDecimal, BigDecimal> bids = new TreeMap<>((k1, k2) -> -k1.compareTo(k2));
    
    public OrderBook(TreeMap<BigDecimal, BigDecimal> asks, TreeMap<BigDecimal, BigDecimal> bids) {
        this.asks.putAll(asks);
        this.bids.putAll(bids);
    }

    @Override
    public String toString() {
        return "Order book:\n\tasks: " + asks.entrySet().stream().map(e -> e.getKey() + " -> " + e.getValue()).collect(Collectors.joining(","))
                + "\n\tbids: " + bids.entrySet().stream().map(e -> e.getKey() + " -> " + e.getValue()).collect(Collectors.joining(","));
    }
    
    
}
