import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import bittrex.BittrexWS;
import bittrex.CurrencyPair;
import bittrex.Trade;


public class Main {

    public static void main(String[] args) throws IOException {
        BittrexWS ws = new BittrexWS();
        
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("type 'o XXX/YYY' to fetch order book.");
        System.out.println("type 't XXX/YYY' to fetch last trades.");
        System.out.println("where XXX: base currency, YYY: counter currency, i.e. BTC/USDT -> bittrex market USDT-BTC");
        do {
            String readLine = br.readLine();
            if (readLine.length() > 0) {
                try {
                    switch (readLine.charAt(0)) {
                    case 'o':
                        System.out.println(ws.getOrderBook(new CurrencyPair(readLine.substring(2))));    
                        break;
                    case 't':
                        System.out.println("Trades: \n\t" + ws.getTrades(new CurrencyPair(readLine.substring(2))).stream().map(Trade::toString).collect(Collectors.joining("\n\t")));    
                        break;
                    default:
                        break;
                    }
                    
                } catch (Throwable t) {
                    System.out.println("failed " + t.getMessage());
                }
            }
        } while(true);
    }
}
