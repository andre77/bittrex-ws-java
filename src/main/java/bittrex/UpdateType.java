package bittrex;

import com.google.gson.annotations.SerializedName;

public enum UpdateType {
    @SerializedName("0")
    ADD,
    @SerializedName("1")
    REMOVE,
    @SerializedName("2")
    UPDATE
 }