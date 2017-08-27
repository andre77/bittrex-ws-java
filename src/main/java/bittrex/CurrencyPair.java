package bittrex;


public class CurrencyPair {

    public final String base;
    public final String counter;

    public CurrencyPair(String base, String counter) {
        this.base = base;
        this.counter = counter;
    }
    
    public CurrencyPair(String currencyPair) {
        int split = currencyPair.indexOf('/');
        if (split < 1) {
          throw new IllegalArgumentException("Could not parse currency pair from '" + currencyPair + "'");
        }
        this.base = currencyPair.substring(0, split);
        this.counter = currencyPair.substring(split + 1);
    }
    

    @Override
    public String toString() {
        return base + "/" + counter;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((base == null) ? 0 : base.hashCode());
        result = prime * result + ((counter == null) ? 0 : counter.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CurrencyPair other = (CurrencyPair) obj;
        if (base == null) {
            if (other.base != null)
                return false;
        } else if (!base.equals(other.base))
            return false;
        if (counter == null) {
            if (other.counter != null)
                return false;
        } else if (!counter.equals(other.counter))
            return false;
        return true;
    }
    
}
