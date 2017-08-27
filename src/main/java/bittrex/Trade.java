package bittrex;

import java.math.BigDecimal;
import java.util.Date;

public class Trade {

    public final OrderType ordeType;
    public final BigDecimal quantity;
    public final CurrencyPair pair;
    public final BigDecimal price;
    private final Date timeStamp;
    public final String id;
    
    public Trade(OrderType ordeType, BigDecimal quantity, CurrencyPair pair, BigDecimal price, Date timeStamp) {
        this(ordeType, quantity, pair, price, timeStamp, null);
    }

    public Trade(OrderType ordeType, BigDecimal quantity, CurrencyPair pair, BigDecimal price, Date timeStamp, String id) {
        this.ordeType = ordeType;
        this.quantity = quantity;
        this.pair = pair;
        this.price = price;
        this.timeStamp = timeStamp;
        this.id = id;
    }

    public Date getTimeStamp() {
        return new Date(timeStamp.getTime());
    }

    @Override
    public String toString() {
        return "Trade [ordeType=" + ordeType + ", quantity=" + quantity + ", pair=" + pair + ", price=" + price + ", timeStamp="
                + timeStamp + ", id=" + id + "]";
    }

    
}
