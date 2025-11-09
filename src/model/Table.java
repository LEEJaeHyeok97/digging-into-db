package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Table implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private List<String> columns;
    private List<Record> records;

    public Table(String name, List<String> columns) {
        this.name = name;
        this.columns = new ArrayList<>(columns);
        this.records = new ArrayList<>();
    }

    public String getName() {
        return this.name;
    }

    public List<String> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    public void insertRecord(Record record) {
        records.add(record);
    }

    public List<Record> selectAll() {
        return Collections.unmodifiableList(records);
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
}
