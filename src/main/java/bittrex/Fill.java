package bittrex;

import java.math.BigDecimal;
import java.util.Date;

import com.google.gson.annotations.SerializedName;

class Fill {
    @SerializedName("Id")
    protected long id;
    @SerializedName("TimeStamp")
    protected Date timeStamp;
    @SerializedName("Quantity")
    protected BigDecimal quantity;
    @SerializedName("Price")
    protected BigDecimal price;
    @SerializedName("Total")
    protected BigDecimal total;
    @SerializedName("FillType")
    protected String fillType;
    @SerializedName("OrderType")
    protected String orderType;         // SELL or BUY
    @Override
    public String toString() {
        return "Fill [id=" + id + ", timeStamp=" + timeStamp + ", quantity=" + quantity + ", price=" + price + ", total="
                + total + ", fillType=" + fillType + ", orderType=" + orderType + "]";
    }
}