package bittrex;

import java.util.Arrays;

import com.google.gson.annotations.SerializedName;

class QueryExchangeState {
    @SerializedName("MarketName")
    protected String marketName;      // always null, do not use it!
    @SerializedName("Nounce")
    protected long nounce;
    @SerializedName("Buys")
    protected Order[] buys;
    @SerializedName("Sells")
    protected Order[] sells;
    @SerializedName("Fills")
    protected Fill[] fills;
    @Override
    public String toString() {
        return "QueryExchangeState [marketName=" + marketName + ", nounce=" + nounce + ", buys=" + Arrays.toString(buys)
                + ", sells=" + Arrays.toString(sells) + ", fills=" + Arrays.toString(fills) + "]";
    }
    
    
}