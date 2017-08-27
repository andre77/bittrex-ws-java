package bittrex;

import java.math.BigDecimal;

import com.google.gson.annotations.SerializedName;

class Order {
    @SerializedName("Quantity")
    protected BigDecimal quantity;
    @SerializedName("Rate")
    protected BigDecimal rate;
    @Override
    public String toString() {
        return "Order [quantity=" + quantity + ", rate=" + rate + "]";
    }
    
}