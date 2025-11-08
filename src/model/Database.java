package model;

import java.util.HashMap;
import java.util.Map;

public class Database {
    private Map<String, Table> tables = new HashMap<>();

    public Database(Map<String, Table> tables) {
        this.tables = tables;
    }

    public void addTable(Table table) {
        tables.put(table.getName(), table);
    }
}
