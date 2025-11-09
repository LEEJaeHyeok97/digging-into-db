package model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public class Record implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, String> values;

    public Record(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    public String get(String column) {
        return values.get(column);
    }

    public Map<String, String> values() {
        return Collections.unmodifiableMap(values);
    }
}
