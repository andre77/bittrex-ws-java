package bittrex;

import java.util.Arrays;

import com.google.gson.annotations.SerializedName;

class UpdateExchangeStateItem {
    @SerializedName("MarketName")
    protected String marketName;
    @SerializedName("Nounce")
    protected long nounce;
    @SerializedName("Buys")
    protected OrderUpdate[] buys;
    @SerializedName("Sells")
    protected OrderUpdate[] sells;
    @SerializedName("Fills")
    protected FillUpdate[] fills;
    
    @Override
    public String toString() {
        return "UpdateExchangeStateItem [marketName=" + marketName + ", nounce=" + nounce + ", buys=" + Arrays.toString(buys)
                + ", sells=" + Arrays.toString(sells) + ", fills=" + Arrays.toString(fills) + "]";
    }
    
    
}