package model;

import java.util.List;

public class Table {

    private String name;
    private List<String> columns;
    private List<Record> records;

    public Table(String name, List<String> columns, List<Record> records) {
        this.name = name;
        this.columns = columns;
        this.records = records;
    }

    public String getName() {
        return this.name;
    }

    public void insertRecord(Record record) {
        records.add(record);
    }

    public List<Record> selectAll() {
        return records;
    }

    public void updateRecordById(int index, Record newRecord) {
        if (index < 0 && index >= records.size()) {
            throw new IllegalArgumentException("[ERROR] 존재하지 않는 레코드입니다.");
        }
        records.set(index, newRecord);
    }

    public void deleteRecord(int index) {
        if (index < 0 && index >= records.size()) {
            throw new IllegalArgumentException("[ERROR] 존재하지 않는 레코드입니다.");
        }
        records.remove(index);
    }
}
