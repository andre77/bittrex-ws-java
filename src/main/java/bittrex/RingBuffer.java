package bittrex;

import java.util.ArrayList;
import java.util.List;

public class RingBuffer<T> {
    
    private final T[] data;
    private final int total;
    
    private int head = 0;
    private int size = 0;
    
    @SuppressWarnings("unchecked")
    public RingBuffer(int size) {
        data = (T[]) new Object[size];
        this.total = size;
    }
    
    public void add(T t) {
        data[head] = t;
        head = (head + 1) % total;
        if (size < total) {
            size ++;
        }
    }
    
    public List<T> list() {
        // TODO array copy or s.th. like that to optimize ?
        List<T> l = new ArrayList<>(size);
        int idx = (head - 1 + total) % total;
        for (int i = 0; i < size; i ++) {
            l.add(data[idx--]);
            
            if (idx < 0) {
                idx += total;
            }
        }
        return l;
    }
    
    public T last() {
        if (isEmpty()) {
            return null;
        }
        int idx = head - 1;
        if (idx < 0) {
            idx = 0;
        }
        return data[idx];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        head = 0;
        size = 0;
        for (int i = 0; i < data.length; i++) {
            data[i] = null;
        }
    }
}
