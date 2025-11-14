package model;

import java.io.Serializable;
import java.util.Map;

public class Version implements Serializable {

    public final Map<String, String> values;
    public long beginTs;
    public long endTs;

    public Version(Map<String, String> values, long beginTs, long endTs) {
        this.values = values;
        this.beginTs = beginTs;
        this.endTs = endTs;
    }

    public boolean visibleAt(long snap) {
        return beginTs <= snap && snap < endTs;
    }
}
