import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bittrex.BittrexWS;
import bittrex.CurrencyPair;


public class TradeListener {

    private static final Logger LOG = LoggerFactory.getLogger(TradeListener.class);
    
    public static void main(String[] args) throws IOException {
        BittrexWS ws = new BittrexWS();
        ws.addTradesListener(new CurrencyPair("BTC/USDT"), t -> {
            LOG.info("new trade: " + t);
        });
    }
}
