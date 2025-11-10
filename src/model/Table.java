package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Table implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final List<String> columns;
    private final List<Record> records;
    private final String primaryKeyColumn;
    private final Map<String, Record> pkIndex;

    public Table(String name, List<String> columns, String primaryKeyColumn) {
        validatePkInColumn(columns, primaryKeyColumn);
        this.name = name;
        this.columns = new ArrayList<>(columns);
        this.records = new ArrayList<>();
        this.primaryKeyColumn = primaryKeyColumn;
        this.pkIndex = new HashMap<>();
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
    }

    public void updateRecordById(int index, Record newRecord) {
        if (index < 0 || index >= records.size()) {
            throw new IllegalArgumentException("[ERROR] 존재하지 않는 레코드입니다.");
        }
        records.set(index, newRecord);
    }

    public void deleteRecord(int index) {
        if (index < 0 || index >= records.size()) {
            throw new IllegalArgumentException("[ERROR] 존재하지 않는 레코드입니다.");
        }
        records.remove(index);
    }

    public int size() {
        return records.size();
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
