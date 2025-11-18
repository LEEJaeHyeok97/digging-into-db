package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import model.index.BPlusTree;
import model.index.OrderedIndex;

public class Table implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final List<String> columns;
    private final String primaryKeyColumn;
    private final OrderedIndex<String, VersionChain> index = new BPlusTree<>();
    private final OrderedIndex<String, Set<String>> idxName = new BPlusTree<>();

    public Table(String name, List<String> columns, String primaryKeyColumn) {
        validatePkInColumn(columns, primaryKeyColumn);
        this.name = name;
        this.columns = columns;
        this.primaryKeyColumn = primaryKeyColumn;
    }

    public String getName() {
        return name;
    }

    public List<String> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    public String getPrimaryKeyColumn() {
        return primaryKeyColumn;
    }

    public Record selectByIdAt(String key, long snapTs) {
        VersionChain chain = index.get(key);
        if (chain == null) {
            return null;
        }

        Version version = chain.visibleAt(snapTs);
        if (version == null) {
            return null;
        }

        return new Record(version.values);
    }

    public List<Record> selectAllAt(long snapTs) {
        var out = new java.util.ArrayList<Record>();
        for (var e : index.entries()) {
            var v = e.getValue().visibleAt(snapTs);
            if (v != null) out.add(new Record(v.values));
        }
        return out;
    }

    public List<Record> findAllByPkBetweenAt(String from, boolean fromInc, String to, boolean toInc, long snapTs) {
        ArrayList<Record> out = new ArrayList<>();
        for (var e : index.range(from, fromInc, to, toInc)) {
            Version v = e.getValue().visibleAt(snapTs);
            if (v != null) out.add(new Record(v.values));
        }
        return out;
    }

    public void insertCommitted(Record record, long ts) {
        String key = requirePk(record);
        VersionChain ch = index.get(key);
        if (ch == null) {
            ch = new VersionChain();
            index.put(key, ch);
        }
        if (ch.alive()) throw new IllegalArgumentException("[ERROR] PK 중복");
        ch.commitInsert(record.values(), ts);
        secAddName(record.get("name"), key);
    }

    public void updateCommitted(String key, Record newRecord, long ts) {
        validatePkNotChanged(key, newRecord);
        VersionChain ch = index.get(key);
        if (ch == null || !ch.alive()) throw new IllegalArgumentException("[ERROR] 존재하지 않는 레코드");

        Version old = ch.latest();
        String oldName = old.values.get("name");

        ch.commitUpdate(newRecord.values(), ts);

        String newName = newRecord.get("name");
        if (Objects.equals(oldName, newName)) {
            secRemoveName(oldName, key);
            secAddName(newName, key);
        }
    }

    public void deleteCommitted(String key, long ts) {
        VersionChain ch = index.get(key);
        if (ch == null || !ch.alive()) throw new IllegalArgumentException("[ERROR] 존재하지 않는 레코드");
        Version old = ch.latest();

        ch.commitDelete(ts);
        secRemoveName(old.values.get("name"), key);
    }

    public List<Record> findAllByAt(String column, String value, long snapTs) {
        if ("name".equals(column)) {
            ArrayList<Record> out = new ArrayList<>();
            Set<String> set = idxName.get(value);
            if (set == null) return out;

            for (String pk : set) {
                VersionChain chain = index.get(pk);
                if (chain == null) continue;
                Version v = chain.visibleAt(snapTs);
                if (v != null && Objects.equals(v.values.get("name"), value)) {
                    out.add(new Record(v.values));
                }
            }

            return out;
        }

        ArrayList<Record> out = new ArrayList<>();
        for (var e : index.entries()) {
            Version v = e.getValue().visibleAt(snapTs);
            if (v != null && Objects.equals(v.values.get(column), value)) {
                out.add(new Record(v.values));
            }
        }

        return out;
    }

    private void secAddName(String name, String pk) {
        if (name == null) {
            return;
        }

        Set<String> set = idxName.get(name);
        if (set == null) {
            set = new LinkedHashSet<>();
            idxName.put(name, set);
        }

        set.add(pk);
    }

    private void secRemoveName(String name, String pk) {
        if (name == null) {
            return;
        }

        Set<String> set = idxName.get(name);
        if (set != null) {
            set.remove(pk);
            if (set.isEmpty()) {
                idxName.remove(name);
            }
        }
    }

    private void validateContainsColumn(String column) {
        if (!columns.contains(column)) {
            throw new IllegalArgumentException("[ERROR] 해당 칼럼을 찾을 수 없습니다.");
        }
    }

    private void validatePkNotChanged(String key, Record newRec) {
        String newKey = newRec.get(primaryKeyColumn);
        if (!Objects.equals(key, newKey)) throw new IllegalArgumentException("[ERROR] PK는 변경할 수 없습니다.");
    }

    private static void validatePkInColumn(List<String> columns, String primaryKeyColumn) {
        if (!columns.contains(primaryKeyColumn)) {
            throw new IllegalArgumentException("[ERROR] PK는 반드시 컬럼에 존재해야 합니다.");
        }
    }

    private String requirePk(Record record) {
        String key = record.get(primaryKeyColumn);
        validateRequirePk(key);
        return key;
    }

    private static void validateRequirePk(String key) {
        if (key == null) {
            throw new IllegalArgumentException("[ERROR] PK 값이 없습니다.");
        }
    }
}
