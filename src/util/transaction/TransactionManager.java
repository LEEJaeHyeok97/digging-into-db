package util.transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import model.Database;
import model.Record;
import model.Table;
import util.wal.Wal;
import util.wal.WalEntry;
import util.wal.WalEntry.Op;

public class TransactionManager {

    private final Database db;
    private final Wal wal;
    private long nextTxId = 1;
    private Tx current;

    static class Tx {
        long id;
        List<WalEntry> ops = new ArrayList<>();
    }

    public TransactionManager(Database db, Wal wal) {
        this.db = db;
        this.wal = wal;
    }

    public boolean isActive() {
        return current != null;
    }

    public void begin() {
        if (current != null) {
            throw new IllegalStateException("[ERROR] 이미 트랜잭션이 진행 중입니다.");
        }

        current = new Tx();
        current.id = nextTxId++;
    }

    public void insert(String tableName, Record record) {
        ensureActive();
        Table table = mustTable(tableName);
        String pkColumn = table.getPrimaryKeyColumn();
        String pk = record.get(pkColumn);

        if (pk == null) {
            throw new IllegalArgumentException("[ERROR] PK 값이 없습니다.");
        }

        current.ops.add(new WalEntry(current.id, Op.INSERT, tableName, pk, record.values()));
    }

    public void update(String tableName, String pk, Record newRecord) {
        ensureActive();
        Table table = mustTable(tableName);
        String primaryKeyColumn = table.getPrimaryKeyColumn();
        String newPk = newRecord.get(primaryKeyColumn);

        if (!Objects.equals(pk, newPk)) {
            throw new IllegalArgumentException("[ERROR] PK는 변경할 수 없습니다.");
        }

        current.ops.add(new WalEntry(current.id, Op.UPDATE, tableName, pk, newRecord.values()));
    }

    public void delete(String tableName, String pk) {
        ensureActive();
        current.ops.add(new WalEntry(current.id, Op.DELETE, tableName, pk, null));
    }
    
    public void commit() throws IOException {
        ensureActive();
        long id = current.id;

        wal.append(new WalEntry(id, Op.BEGIN, null, null, null));
        for (WalEntry e : current.ops) {
            wal.append(e);
        }

        wal.append(new WalEntry(id, Op.COMMIT, null, null, null));

        for (WalEntry e : current.ops) {
            applyOne(db, e, false);
        }

        current = null;
    }

    public void rollback() {
        ensureActive();
        current = null; // 버퍼 폐기
    }

    public static void recover(Database db, List<WalEntry> log) {
        LinkedHashMap<Long, List<WalEntry>> byTx = new LinkedHashMap<>();
        Set<Long> committed = new HashSet<>();

        for (WalEntry e : log) {
            if (e.op == Op.BEGIN) {
                byTx.put(e.txId, new ArrayList<>());
            } else if (e.op == Op.COMMIT) {
                committed.add(e.txId);
            } else {
                byTx.computeIfAbsent(e.txId, k -> new ArrayList<>()).add(e);
            }
        }

        for (var entry : byTx.entrySet()) {
            long tx = entry.getKey();
            if (!committed.contains(tx)) continue;

            for (WalEntry e : entry.getValue()) {
                applyOne(db, e, true);
            }
        }
    }

    private void ensureActive() {
        if (current == null) {
            throw new IllegalStateException("[ERROR] BEGIN 먼저 호출하세요.");
        }
    }

    private Table mustTable(String name) {
        Table table = db.getTable(name);
        if (table == null) {
            throw new IllegalArgumentException("[ERROR] 테이블이 없습니다: " + name);
        }

        return table;
    }

    private static void applyOne(Database db, WalEntry e, boolean tolerant) {
        Table table = db.getTable(e.table);
        if (table == null) return;

        try {
            switch (e.op) {
                case INSERT -> table.insertRecord(new Record(e.values));
                case UPDATE -> table.updateById(e.pk, new Record(e.values));
                case DELETE -> table.deleteById(e.pk);
                default -> {
                }
            }
        } catch (Exception exception) {
            if (!tolerant) {
                throw exception;
            }
        }
    }
}
