package bittrex;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import microsoft.aspnet.signalr.client.ConnectionState;
import microsoft.aspnet.signalr.client.LogLevel;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.transport.WebsocketTransport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BittrexWS {

    private static final Logger LOG = LoggerFactory.getLogger(BittrexWS.class);
    private static final String DEFAULT_SERVER_URL = "https://www.bittrex.com";
    
    private static final microsoft.aspnet.signalr.client.Logger logger = (message, level) -> {
        // ignore all levels but critical
        if (level == LogLevel.Critical) {
            LOG.warn(message);
        }
    };
    
    
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    
    private HubConnection connection;
    private HubProxy proxy;
    private Map<String, ChannelHandler> handlers = new ConcurrentHashMap<>();
    
    Map<CurrencyPair, Object> locks = new ConcurrentHashMap<>();

    private Date connectionTimestamp;
    private WebsocketState state = WebsocketState.DISCONNECTED;
    
    public BittrexWS() {
        //reconnect();
    }
    
    
    private void init() throws InterruptedException, ExecutionException {
        LOG.info("BittrexWS: Going to (re)connect.");
        handlers.clear();
        if (connection != null) {
            try {
                connection.stop();
            } catch (Exception ignored) {}
        }
        connectionTimestamp = null;
        state = WebsocketState.CONNECTING;
        connection = new HubConnection(DEFAULT_SERVER_URL, null, true, logger);
        connection.error(error -> LOG.warn("There was an error communicating with the server.", error));
        connection.connected(() -> {
            LOG.info("Connecton started");
            connectionTimestamp = new Date();
            state = WebsocketState.CONNECTED;
        });
        connection.closed(() -> {
            LOG.info("Connecton closed");
            connectionTimestamp = null;
            state = WebsocketState.DISCONNECTED;
        });
        
        proxy = connection.createHubProxy("corehub");

        proxy.subscribe(new Object() {
              @SuppressWarnings("unused")
              public void updateSummaryState(Object o) {
                  // ignore it for now
              }
              
              @SuppressWarnings("unused")
              public void updateExchangeState(UpdateExchangeStateItem o) {
                  ChannelHandler channelHandler = handlers.get(o.marketName);
                  if (channelHandler != null) {
                      try {
                          channelHandler.processUpdate(o);
                      } catch (Throwable t) {
                          LOG.warn("Error processing update " + o.marketName, t);
                          throw t;
                      }
                  } else {
                      LOG.warn("Received update for unknown handler, market: " + o.marketName);
                  }
              }
          });
        
        SignalRFuture<Void> start = connection.start(new WebsocketTransport(logger));
        start.get();
    }

    private <T> T useHandler(CurrencyPair pair, Function<ChannelHandler, T> f) throws Exception {
        if (connection == null || connection.getState() != ConnectionState.Connected) {
            reconnect0();
        }
        if (reconnecting.get()) {
           throw new RuntimeException("Bittrex WebSocket is reconnecting...");
        }
        if (connection.getState() != ConnectionState.Connected) {
            throw new RuntimeException("Bittrex WebSocket is not connected, current state: " + connection.getState());
        }
        
        Object lock = locks.get(pair);
        if (lock == null) {
            synchronized (locks) {
                lock = locks.get(pair);
                if (lock == null) {
                    lock = new Object();
                    locks.put(pair, lock);
                }
            }
        }
        try {
            ChannelHandler ch;
            synchronized (lock) {
                ch = subscribe(pair);
            }
            return f.apply(ch);
        } catch (BittrexException e) {
            if (e.isReconnect())  {
                LOG.warn("Error, will reconnect.", e);
                reconnect0();
            } else if (e.isResubscribe()) {
                LOG.warn("Error, will resubscribe " + pair, e);
                handlers.remove(toBittrexMarket(pair));
            }
            throw e;
        } catch (Throwable t) {
            LOG.warn("Error " + pair, t);
            throw t;
        }
    }
    
    public OrderBook getOrderBook(CurrencyPair pair) throws Exception {
        return useHandler(pair, h -> {
            OrderBook result = h.getOrderBook();
            return result;
        });
    }
    
    public List<Trade> getTrades(CurrencyPair pair) throws Exception {
        return useHandler(pair, h -> h.getTrades());
    }
    
    private void reconnect0() {
        if (!reconnecting.compareAndSet(false, true)) {
            return;
        }
        state = WebsocketState.CONNECTING;
        connectionTimestamp = null;
        try {
            init();
            state = WebsocketState.CONNECTED;
            connectionTimestamp = new Date();
        } catch (Exception e) {
            state = WebsocketState.ERROR;
            throw new RuntimeException(e);
        } finally {
            reconnecting.set(false);
            synchronized (reconnecting) {
                reconnecting.notifyAll();
            }
        }
    }
    
    private ChannelHandler subscribe(final CurrencyPair pair) throws InterruptedException, ExecutionException {
        final String marketName = toBittrexMarket(pair);
        ChannelHandler handler = handlers.get(marketName);
        if (handler != null) {
            return handler;
        }
        
        final ChannelHandler h = new ChannelHandler(marketName, pair, proxy);
        handlers.put(marketName, h);
        
        SignalRFuture<Void> updates;
        try {
            updates = proxy.invoke("SubscribeToExchangeDeltas", marketName);
        } catch (Throwable e) {
            LOG.warn("Could not subscribe to " + marketName + ", going to reconnect.", e);
            reconnect();
            throw new RuntimeException();
        }
        updates.onError(err -> {
            LOG.warn("Could not subscribe for exchange deltas " + marketName, err);
            handlers.remove(marketName);
        });
        try {
            updates.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException err) {
            handlers.remove(marketName);
            LOG.warn("Could not subscribe for exchange deltas " + marketName + " (timeout).", err);
            throw new RuntimeException("Could not subscribe for exchange deltas " + marketName + " (timeout).", err);
        }
        
        h.fetchState();
        LOG.info("Subscribed successfully for " + pair);
        return h;
    }
    
    protected static String toBittrexMarket(CurrencyPair pair) {
        return pair.counter + "-" + pair.base;
    }
    
    public Date connectionTimestamp() {
        return this.connectionTimestamp;
    }
    

    public void reconnect() {
        reconnect0();
    }

    public synchronized List<ChannelHandler> getChannels() {
        return handlers.values().stream().collect(Collectors.toList());
    }

    public synchronized void resubscribe(String channelId) {
        handlers.remove(channelId);
    }
    
    private static enum WebsocketState {
        CONNECTED, CONNECTING, ERROR, DISCONNECTED;
    }
}
