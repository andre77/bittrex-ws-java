package bittrex;


@SuppressWarnings("serial")
public class BittrexException extends RuntimeException {

    private final boolean reconnect;
    private final boolean resubscribe;
    
    public BittrexException(boolean reconnect, String msg, boolean resubscribe) {
        super(msg);
        this.reconnect = reconnect;
        this.resubscribe = resubscribe;
    }
    
    public BittrexException(boolean reconnect, String msg) {
        this(reconnect, msg, false);
    }

    public boolean isReconnect() {
        return reconnect;
    }

    public boolean isResubscribe() {
        return resubscribe;
    }
    
    
}
