package tk.zielony.taskqueue;

/**
 * Created by Marcin on 2016-04-10.
 */
public class AverageValue {
    double value = 0;
    int values = 0;

    public void add(long v) {
        if (v < 10 || v > 100)
            return;
        if (values == 0) {
            value = v;
            values = 1;
        } else {
            value = (value * values + v) / (values + 1);
            values = Math.min(values + 1, 100);
        }
    }

    public long get() {
        return (long) value;
    }
}
