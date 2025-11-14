package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VersionChain implements Serializable {

    private final List<Version> vs = new ArrayList<>();

    public Version visibleAt(long snap) {
        for (int i = vs.size() - 1; i >= 0; i--) {
            Version v = vs.get(i);
            if (v.visibleAt(snap)) return v;
        }
        return null;
    }

    public boolean alive() {
        return !vs.isEmpty() && vs.get(vs.size() - 1).endTs == Long.MAX_VALUE;
    }

    public void commitInsert(Map<String, String> v, long ts) {
        vs.add(new Version(v, ts, Long.MAX_VALUE));
    }

    public void commitUpdate(Map<String,String> v, long ts) {
        validateIsEmpty("[ERROR] UPDATE 대상 없음");
        vs.get(vs.size()-1).endTs = ts;
        vs.add(new Version(v, ts, Long.MAX_VALUE));
    }

    public void commitDelete(long ts) {
        validateIsEmpty("[ERROR] DELETE 대상 없음");
        vs.get(vs.size()-1).endTs = ts;
    }

    private void validateIsEmpty(String s) {
        if (vs.isEmpty()) {
            throw new IllegalStateException(s);
        }
    }
}
