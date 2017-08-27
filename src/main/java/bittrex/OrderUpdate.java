package bittrex;

import java.math.BigDecimal;

import com.google.gson.annotations.SerializedName;

class OrderUpdate {
//        "Type":2,"Rate":0.10554292,"Quantity":0.60946257
        @SerializedName("Type")
        protected UpdateType type;
        @SerializedName("Rate")
        protected BigDecimal rate;
        @SerializedName("Quantity")
        protected BigDecimal quantity;
        @Override
        public String toString() {
            return "OrderUpdate [type=" + type + ", rate=" + rate + ", quantity=" + quantity + "]";
        }
    }