package bittrex;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.hubs.HubProxy;

public class ChannelHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(ChannelHandler.class);
    
    private final CurrencyPair pair;
    private WebsocketChannelState state = WebsocketChannelState.SYNCING;
    
    // price -> amount
    private final TreeMap<BigDecimal, BigDecimal> bids = new TreeMap<>((k1, k2) -> -k1.compareTo(k2));
    private final TreeMap<BigDecimal, BigDecimal> asks = new TreeMap<>();
    private final HubProxy proxy;
    private final String marketName;
    
    private OrderBook orderBook = null;

    private long heartbeat;
    private Date syncTimestamp;
    private long nounce;
    
    private final List<UpdateExchangeStateItem> queue = new ArrayList<>();
    private final RingBuffer<Trade> tradeRing = new RingBuffer<Trade>(1000);
    private final Set<Consumer<Trade>> tradeListener;
    
    public ChannelHandler(String marketName, CurrencyPair pair, HubProxy proxy, Set<Consumer<Trade>> tradeListener) {
        this.pair = pair;
        this.proxy = proxy;
        this.marketName = marketName;
        this.tradeListener = tradeListener;
    }
    
    protected void fetchState() {
        SignalRFuture<QueryExchangeState> state = proxy.invoke(QueryExchangeState.class, "QueryExchangeState", marketName);
        
        try {
            QueryExchangeState queryExchangeState = state.get(10, TimeUnit.SECONDS);
            if (queryExchangeState == null) {
                throw new RuntimeException("Exchange State in null, pair " + pair + ".");
            }
            processSnapShot(queryExchangeState);
            getOrderBook();
        } catch (Throwable e) {
            LOG.warn("Could not fetch the snapshot " + e.getClass().getSimpleName(), e);
            this.state = WebsocketChannelState.ERROR;
            throw new BittrexException(false, "Could not fetch the snapshot. " + e.getClass().getSimpleName()  + ": "+ e.getMessage(), true);
        }
    }
    
    synchronized protected void processUpdate(UpdateExchangeStateItem o) {
        orderBook = null;
        if (state == WebsocketChannelState.SYNCING) {
            queue.add(o);
        } else {
            processUpdate0(o);
        }
        heartbeat = System.currentTimeMillis();
    }
    
    private void processUpdate0(UpdateExchangeStateItem o) {
        if (o.nounce <= nounce) {
            return;
        }
        if (o.nounce - nounce == 1) {
            nounce++;
        } else {
            LOG.warn("Missing data, going to resubscribe " + pair);
            state = WebsocketChannelState.ERROR;
            syncTimestamp = null;
            orderBook = null;
            return;
        }
        
        BiConsumer<OrderUpdate, TreeMap<BigDecimal, BigDecimal>> ordersProcessor = (u, col) -> {
            switch (u.type) {
            case ADD:
            case UPDATE:
                col.put(u.rate, u.quantity);
                break;
            case REMOVE:
                col.remove(u.rate);
                break;
            default:
                throw new RuntimeException("Unknown update type " + u.type);    // should never happen
            }  
        };
        Stream.of(o.buys).forEach(u -> ordersProcessor.accept(u, bids));
        Stream.of(o.sells).forEach(u -> ordersProcessor.accept(u, asks));
        Stream.of(o.fills).forEach(u -> {
            OrderType ordeType =  u.orderType.equals("SELL") ? OrderType.ASK : OrderType.BID;
            Trade t = new Trade(ordeType, u.quantity, pair, u.rate, u.timeStamp, null);
            tradeRing.add(t);
            informListener(t);
        });
        
      
    }
    
    private void informListener(Trade trade) {
        try {
            tradeListener.parallelStream().forEach(c -> c.accept(trade));
        } catch (Throwable t) {
            LOG.warn("Error executing listeners.", t);
        }
    }

    private synchronized void processSnapShot(QueryExchangeState v) {
        nounce = v.nounce;
        bids.clear();
        asks.clear();
        Stream.of(v.buys).forEach(o -> bids.put(o.rate, o.quantity));
        Stream.of(v.sells).forEach(o -> asks.put(o.rate, o.quantity));
        queue.forEach(this::processUpdate0);
        queue.clear();
        
        tradeRing.clear();
        Stream.of(v.fills).forEach(f -> {
            String id = Long.toString(f.id);
            OrderType ordeType =  f.orderType.equals("SELL") ? OrderType.ASK : OrderType.BID;
            tradeRing.add(new Trade(ordeType, f.quantity, pair, f.price, f.timeStamp, id));
        });
        
        // inform trde listeners about the very last trade
        Trade last = tradeRing.last();
        if (last != null) {
            informListener(last);
        }
        
        state = WebsocketChannelState.SYNCED;
        heartbeat = System.currentTimeMillis();
        syncTimestamp = new Date();
    }

    public OrderBook getOrderBook() {
        checkState();
        
        OrderBook old = orderBook;
        if (old != null) {
            return old;
        }
        
        synchronized (this) {
            orderBook = new OrderBook(asks, bids);
            checkConsistency(orderBook);
            return orderBook;
        }
    }

    private void checkConsistency(OrderBook orderBook) {
        if (orderBook.bids.isEmpty()) {
            throw new BittrexException(false, String.format("Order book inconsistent, pair: %s, bid site is empty.", pair), true);
        }
        if (orderBook.asks.isEmpty()) {
            throw new BittrexException(false, String.format("Order book inconsistent, pair: %s, ask site is empty.", pair), true);
        }
        if (gt(orderBook.bids.firstKey(), orderBook.asks.firstKey())) {
            throw new BittrexException(false, String.format("Order book inconsistent, pair: %s, first bid %s is higher than first ask %s.", pair, orderBook.bids.firstKey(), orderBook.asks.firstKey()), true);
        }        
    }
    
    public static boolean gt(BigDecimal a, BigDecimal b) { return a.compareTo(b) > 0; }
    

    public List<Trade> getTrades() {
        checkState();
        synchronized (this) {
            return tradeRing.list();
        }
    }

    private void checkState() {
        if (!state.synced()) {
            throw new BittrexException(false, "Channel is not synced @ bittrex, pair: " + pair + ", state: " + state, state == WebsocketChannelState.ERROR);
        } else if(System.currentTimeMillis() - heartbeat > TimeUnit.SECONDS.toMillis(60)) {
            throw new BittrexException(false, "Channel has not received updates @ bittrex, pair: " + pair + ", state: " + state, true);
        }
    }

    public String getId() {
        return marketName;
    }

    public WebsocketChannelState getState() {
        return state;
    }

    public CurrencyPair getPair() {
        return pair;
    }
    public Date getSyncTimestamp() {
        return syncTimestamp;
    }

    public int getTimeSinceLastUpdateSeconds() {
        return heartbeat == 0 ? -1 : (int) ((System.currentTimeMillis() - heartbeat) / 1000);
    }

    public static enum WebsocketChannelState {
        SYNCED, SYNCING, ERROR;

        public boolean synced() {
            return this == SYNCED;
        }
    }
}
