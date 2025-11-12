package model.wal;

import java.io.Serializable;
import java.util.Map;

public class WalEntry implements Serializable {

    public enum Op { BEGIN, INSERT, UPDATE, DELETE, COMMIT}

    public final long txId;
    public final Op op;
    public final String table;
    public final String pk;
    public final Map<String, String> values;

    public WalEntry(long txId, Op op, String table, String pk, Map<String, String> values) {
        this.txId = txId;
        this.op = op;
        this.table = table;
        this.pk = pk;
        this.values = values;
    }
}
