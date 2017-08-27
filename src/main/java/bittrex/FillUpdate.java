package bittrex;

import java.math.BigDecimal;
import java.util.Date;

import com.google.gson.annotations.SerializedName;

class FillUpdate {
    //{"OrderType":"BUY","Rate":0.09575999,"Quantity":1.03224938,"TimeStamp":"2017-07-09T21:58:29.743"}
    @SerializedName("OrderType")
    protected String orderType;
    @SerializedName("Rate")
    protected BigDecimal rate;
    @SerializedName("Quantity")
    protected BigDecimal quantity;
    @SerializedName("TimeStamp")
    protected Date timeStamp;
    @Override
    public String toString() {
        return "Fill [orderType=" + orderType + ", rate=" + rate + ", quantity=" + quantity + ", timeStamp=" + timeStamp + "]";
    }
    
    
}