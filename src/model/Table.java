package model;

import java.util.List;

public class Table {

    private String name;
    private List<String> columns;
    private List<Record> records;

    public String getName() {
        return this.name;
    }
}
