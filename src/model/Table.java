package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class Table implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final List<String> columns;
    private final List<Record> records;
    private final String primaryKeyColumn;
    private final Map<String, Record> pkIndex;
    private final NavigableMap<String, Record> pkOrdered;

    public Table(String name, List<String> columns, String primaryKeyColumn) {
        validatePkInColumn(columns, primaryKeyColumn);
        this.name = name;
        this.columns = new ArrayList<>(columns);
        this.records = new ArrayList<>();
        this.primaryKeyColumn = primaryKeyColumn;
        this.pkIndex = new HashMap<>();
        this.pkOrdered = new TreeMap<>();
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

    public List<Record> selectAll() {
        return Collections.unmodifiableList(records);
    }

    public Record selectById(String key) {
        return pkIndex.get(key);
    }

    public void insertRecord(Record record) {
        String key = requirePk(record);
        validateDuplicatedPK(key);
        records.add(record);
        pkIndex.put(key, record);
        pkOrdered.put(key, record);
    }

    public void updateById(String key, Record newRecord) {
        Record oldRecord = pkIndex.get(key);
        validateIsExists(oldRecord);

        String newKey = requirePk(newRecord);
        validateIsDifferentPK(key, newKey);

        int idx = indexOfIdentity(oldRecord);
        records.set(idx, newRecord);
        pkIndex.put(key, newRecord);
        pkOrdered.put(key, newRecord);
    }

    public void deleteById(String key) {
        Record oldRecord = pkIndex.remove(key);
        validateIsNull(oldRecord);
        pkOrdered.remove(key);
        int idx = indexOfIdentity(oldRecord);
        records.remove(idx);
    }

    public List<Record> findAllBy(String column, String value) {
        validateContainsColumn(column);
        ArrayList<Record> out = new ArrayList<>();
        for (Record record : records) {
            if (value.equals(record.get(column))) {
                out.add(record);
            }
        }

        return out;
    }

    public List<Record> findAllByPkBetween(String from, boolean fromInclusive, String to, boolean toInclusive) {
        return new ArrayList<>(pkOrdered.subMap(from, fromInclusive, to, toInclusive).values());
    }

    public int size() {
        return records.size();
    }

    private void validateContainsColumn(String column) {
        if (!columns.contains(column)) {
            throw new IllegalArgumentException("[ERROR] 해당 칼럼을 찾을 수 없습니다.");
        }
    }

    private static void validateIsNull(Record oldRecord) {
        if (oldRecord == null) {
            throw new IllegalArgumentException("[ERROR] 존재하지 않는 레코드입니다.");
        }
    }

    private int indexOfIdentity(Record oldRecord) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i) == oldRecord) {
                return i;
            }
        }

        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).equals(oldRecord)) {
                return i;
            }
        }
        throw new IllegalArgumentException("[ERROR] 인덱스 갱신 실패");
    }

    private static void validateIsDifferentPK(String key, String newKey) {
        if (!key.equals(newKey)) {
            throw new IllegalArgumentException("[ERROR] PK는 변경할 수 없습니다.");
        }
    }

    private static void validateIsExists(Record oldRecord) {
        if (oldRecord == null) {
            throw new IllegalArgumentException("[ERROR] 존재하지 않는 레코드입니다.");
        }
    }

    private void validateDuplicatedPK(String key) {
        if (pkIndex.containsKey(key)) {
            throw new IllegalArgumentException("[ERROR] PK 중복");
        }
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
